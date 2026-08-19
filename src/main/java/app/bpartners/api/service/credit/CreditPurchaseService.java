package app.bpartners.api.service.credit;

import static app.bpartners.api.model.credit.CreditPurchaseOrigin.SELF_SERVICE;
import static app.bpartners.api.model.credit.CreditPurchaseStatus.COMPLETED;
import static app.bpartners.api.model.credit.CreditPurchaseStatus.PENDING;
import static app.bpartners.api.model.credit.CreditTransactionMovementType.CREDIT;
import static app.bpartners.api.model.credit.CreditTransactionType.PURCHASE;
import static java.time.Instant.now;

import app.bpartners.api.model.User;
import app.bpartners.api.model.credit.CreditPack;
import app.bpartners.api.model.credit.CreditPurchase;
import app.bpartners.api.model.credit.CreditPurchaseCharge;
import app.bpartners.api.model.credit.CreditPurchaseSubmission;
import app.bpartners.api.model.credit.CreditTransaction;
import app.bpartners.api.model.credit.CreditUnitPrice;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.ConflictException;
import app.bpartners.api.repository.jpa.CreditPurchaseRepository;
import app.bpartners.api.repository.jpa.CreditTransactionRepository;
import app.bpartners.api.service.subscription.StripeCreditPurchaseService;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreditPurchaseService {
  private final CreditPurchaseRepository creditPurchaseRepository;
  private final CreditTransactionRepository creditTransactionRepository;
  private final CreditService creditService;
  private final CreditLedgerService creditLedgerService;
  private final StripeCreditPurchaseService stripeCreditPurchaseService;

  public CreditPurchase submit(User user, CreditPurchaseSubmission submission) {
    var alreadySubmitted = creditPurchaseRepository.findById(submission.purchaseId());
    if (alreadySubmitted.isPresent()) {
      return alreadySubmittedOrConflict(alreadySubmitted.get(), user, submission);
    }

    var stripeCustomerIdentifier = user.getUserSubscriptionId();
    if (stripeCustomerIdentifier == null) {
      throw new BadRequestException(
          "User.id=" + user.getId() + " is not associated to a stripe customer yet");
    }
    var pending = priced(user, submission);
    var charge =
        user.isPaymentMethodExists()
            ? stripeCreditPurchaseService.chargeOffSession(stripeCustomerIdentifier, pending)
            : CreditPurchaseCharge.noChargeableCard();
    if (charge.succeeded()) {
      return completed(pending);
    }
    log.info(
        "CreditPurchase.id={} could not be charged off session ({}), redirecting to checkout",
        submission.purchaseId(),
        charge.failureCode());
    var redirectionUrl =
        stripeCreditPurchaseService.checkoutSessionUrl(
            stripeCustomerIdentifier, pending, submission.successUrl(), submission.failureUrl());
    return creditPurchaseRepository.save(
        pending.toBuilder()
            .redirectionUrl(redirectionUrl)
            .redirectionSuccessUrl(submission.successUrl())
            .redirectionFailureUrl(submission.failureUrl())
            .build());
  }

  public Optional<CreditPurchase> complete(String purchaseId) {
    var creditPurchase = creditPurchaseRepository.findById(purchaseId);
    if (creditPurchase.isEmpty()) {
      log.warn("No CreditPurchase.id={} to complete, skipping", purchaseId);
      return Optional.empty();
    }
    return creditPurchase.map(
        alreadySubmitted ->
            isAlreadyCompleted(alreadySubmitted) ? alreadySubmitted : completed(alreadySubmitted));
  }

  private boolean isAlreadyCompleted(CreditPurchase creditPurchase) {
    return COMPLETED.equals(creditPurchase.getStatus())
        && creditPurchase.getCreditTransactionId() != null;
  }

  private CreditPurchase completed(CreditPurchase creditPurchase) {
    var creditTransactionId =
        creditTransactionRepository
            .findFirstByCreditPurchaseId(creditPurchase.getId())
            .map(CreditTransaction::getId)
            .orElseGet(() -> grantedCreditsTransactionId(creditPurchase));
    return creditPurchaseRepository.save(
        creditPurchase.toBuilder()
            .status(COMPLETED)
            .completionDatetime(
                creditPurchase.getCompletionDatetime() == null
                    ? now()
                    : creditPurchase.getCompletionDatetime())
            .creditTransactionId(creditTransactionId)
            .build());
  }

  private String grantedCreditsTransactionId(CreditPurchase creditPurchase) {
    return creditLedgerService
        .append(
            CreditTransaction.builder()
                .userId(creditPurchase.getUserId())
                .type(PURCHASE)
                .movementType(CREDIT)
                .credits(creditPurchase.getCredits())
                .label(creditPurchase.paymentLabel())
                .creditPurchaseId(creditPurchase.getId())
                .build())
        .getId();
  }

  private CreditPurchase alreadySubmittedOrConflict(
      CreditPurchase alreadySubmitted, User user, CreditPurchaseSubmission submission) {
    if (!user.getId().equals(alreadySubmitted.getUserId())) {
      throw new ConflictException(
          "CreditPurchase.id=" + submission.purchaseId() + " already exists");
    }
    if (!buysTheSameThing(alreadySubmitted, submission)) {
      throw new ConflictException(
          "CreditPurchase.id="
              + submission.purchaseId()
              + " was already submitted with a different payload, a purchase is immutable");
    }
    return alreadySubmitted;
  }

  private boolean buysTheSameThing(
      CreditPurchase alreadySubmitted, CreditPurchaseSubmission submission) {
    if (!submission.type().equals(alreadySubmitted.getType())) {
      return false;
    }
    return switch (submission.type()) {
      case PACK ->
          alreadySubmitted.getCreditPack() != null
              && Objects.equals(submission.creditPackId(), alreadySubmitted.getCreditPack().getId())
              && submission.quantityOrDefault() == quantityOrDefault(alreadySubmitted);
      case CUSTOM -> Objects.equals(submission.credits(), alreadySubmitted.getCredits());
    };
  }

  private int quantityOrDefault(CreditPurchase creditPurchase) {
    return creditPurchase.getQuantity() == null ? 1 : creditPurchase.getQuantity();
  }

  private CreditPurchase priced(User user, CreditPurchaseSubmission submission) {
    var unitPrice = creditService.resolveCreditUnitPrice(user);
    return switch (submission.type()) {
      case PACK -> pricedPackPurchase(user, submission, unitPrice);
      case CUSTOM -> pricedPurchase(user, submission, unitPrice, null, null, submission.credits());
    };
  }

  private CreditPurchase pricedPackPurchase(
      User user, CreditPurchaseSubmission submission, CreditUnitPrice unitPrice) {
    var creditPack = creditService.getCreditPack(submission.creditPackId());
    if (creditPack.isDeprecated()) {
      throw new BadRequestException(
          "CreditPack(id=" + creditPack.getId() + ") is deprecated and can not be purchased");
    }
    if (creditPack.getCredits() == null) {
      throw new BadRequestException(
          "CreditPack(id="
              + creditPack.getId()
              + ") carries no fixed credits amount, submit a CUSTOM purchase instead");
    }
    var quantity = submission.quantityOrDefault();
    return pricedPurchase(
        user, submission, unitPrice, creditPack, quantity, creditPack.getCredits() * quantity);
  }

  private CreditPurchase pricedPurchase(
      User user,
      CreditPurchaseSubmission submission,
      CreditUnitPrice unitPrice,
      CreditPack creditPack,
      Integer quantity,
      Long credits) {
    return CreditPurchase.builder()
        .id(submission.purchaseId())
        .userId(user.getId())
        .type(submission.type())
        .creditPack(creditPack)
        .quantity(quantity)
        .credits(credits)
        .creditUnitPriceInCentsWithoutVat(unitPrice.inCentsWithoutVat())
        .amountInCentsWithoutVat(unitPrice.totalInCentsWithoutVat(credits))
        .amountInCentsWithVat(unitPrice.totalInCentsWithVat(credits))
        .vatPercent(unitPrice.vatPercent())
        .status(PENDING)
        .origin(SELF_SERVICE)
        .creationDatetime(now())
        .build();
  }
}
