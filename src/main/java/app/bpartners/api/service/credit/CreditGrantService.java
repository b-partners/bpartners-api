package app.bpartners.api.service.credit;

import static app.bpartners.api.model.credit.CreditTransactionMovementType.CREDIT;
import static app.bpartners.api.model.credit.CreditTransactionType.SUBSCRIPTION_GRANT;
import static java.time.Instant.now;

import app.bpartners.api.model.credit.CreditTransaction;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.repository.jpa.CreditTransactionRepository;
import app.bpartners.api.service.utils.TemporalUtils;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreditGrantService {
  private static final String DEFAULT_GRANT_LABEL = "Crédits inclus dans l'abonnement";
  private static final String TRANSITIONAL_GRANT_LABEL =
      "Crédits offerts pendant la transition vers le prépayé";
  private final CreditLedgerService creditLedgerService;
  private final CreditTransactionRepository creditTransactionRepository;
  private final TemporalUtils temporalUtils;

  public Optional<CreditTransaction> grantTransitionalCredits(String userId, long credits) {
    if (credits <= 0) {
      return Optional.empty();
    }
    if (hasLiveTransitionalGrant(userId)) {
      log.info(
          "User(id={}) already has a live transitional credit grant, skipping new grant", userId);
      return Optional.empty();
    }
    var granted =
        creditLedgerService.append(
            CreditTransaction.builder()
                .userId(userId)
                .type(SUBSCRIPTION_GRANT)
                .movementType(CREDIT)
                .credits(credits)
                .label(TRANSITIONAL_GRANT_LABEL)
                .subscriptionProductId(null)
                .grantPeriodStart(temporalUtils.startOfActualMonth())
                .expirationDatetime(temporalUtils.startOfNextMonthInstant())
                .build());
    log.info(
        "Granted {} transitional credits to User(id={}), CreditTransaction.id={}",
        credits,
        userId,
        granted.getId());
    return Optional.of(granted);
  }

  @Transactional
  public void revokeTransitionalGrants(String userId) {
    creditTransactionRepository.acquireWalletLock(userId);
    var now = now();
    var liveTransitionalGrants =
        liveTransitionalGrants(userId, now).stream()
            .map(transaction -> transaction.toBuilder().expirationDatetime(now).build())
            .toList();
    if (liveTransitionalGrants.isEmpty()) {
      return;
    }
    creditTransactionRepository.saveAll(liveTransitionalGrants);
    log.info(
        "Revoked {} transitional credit grant(s) of User(id={}) following a new subscription",
        liveTransitionalGrants.size(),
        userId);
  }

  private boolean hasLiveTransitionalGrant(String userId) {
    return !liveTransitionalGrants(userId, now()).isEmpty();
  }

  private List<CreditTransaction> liveTransitionalGrants(String userId, Instant at) {
    return creditTransactionRepository
        .findAllByUserIdAndTypeAndSubscriptionProductIdIsNull(userId, SUBSCRIPTION_GRANT)
        .stream()
        .filter(transaction -> !transaction.isExpiredAt(at))
        .toList();
  }

  public Optional<CreditTransaction> grantIncludedCredits(String userId, SubscriptionProduct plan) {
    var includedCredits = plan.includedCreditsPerBillingPeriodOrDefault();
    if (includedCredits <= 0) {
      log.info(
          "SubscriptionProduct(id={}) includes no credit, nothing granted to User(id={})",
          plan.getId(),
          userId);
      return Optional.empty();
    }
    var billingPeriodStart = temporalUtils.startOfActualMonth();
    if (alreadyGranted(userId, plan.getId(), billingPeriodStart)) {
      log.info(
          "User(id={}) was already granted the credits included in SubscriptionProduct(id={}) for"
              + " the billing period starting on {}, skipping",
          userId,
          plan.getId(),
          billingPeriodStart);
      return Optional.empty();
    }
    var granted =
        creditLedgerService.append(
            CreditTransaction.builder()
                .userId(userId)
                .type(SUBSCRIPTION_GRANT)
                .movementType(CREDIT)
                .credits(includedCredits)
                .label(grantLabel(plan))
                .subscriptionProductId(plan.getId())
                .grantPeriodStart(billingPeriodStart)
                .expirationDatetime(temporalUtils.startOfNextMonthInstant())
                .build());
    log.info(
        "Granted {} credits to User(id={}) from SubscriptionProduct(id={}) for the billing period"
            + " starting on {}, CreditTransaction.id={}",
        includedCredits,
        userId,
        plan.getId(),
        billingPeriodStart,
        granted.getId());
    return Optional.of(granted);
  }

  private boolean alreadyGranted(
      String userId, String subscriptionProductId, LocalDate billingPeriodStart) {
    return creditTransactionRepository
        .existsByUserIdAndTypeAndSubscriptionProductIdAndGrantPeriodStart(
            userId, SUBSCRIPTION_GRANT, subscriptionProductId, billingPeriodStart);
  }

  private String grantLabel(SubscriptionProduct plan) {
    return plan.getName() == null
        ? DEFAULT_GRANT_LABEL
        : DEFAULT_GRANT_LABEL + " " + plan.getName();
  }
}
