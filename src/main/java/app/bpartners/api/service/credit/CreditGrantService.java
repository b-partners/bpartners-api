package app.bpartners.api.service.credit;

import static app.bpartners.api.model.credit.CreditTransactionMovementType.CREDIT;
import static app.bpartners.api.model.credit.CreditTransactionType.SUBSCRIPTION_GRANT;

import app.bpartners.api.model.credit.CreditTransaction;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.repository.jpa.CreditTransactionRepository;
import app.bpartners.api.service.utils.TemporalUtils;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CreditGrantService {
  private static final String DEFAULT_GRANT_LABEL = "Crédits inclus dans l'abonnement";
  private final CreditLedgerService creditLedgerService;
  private final CreditTransactionRepository creditTransactionRepository;
  private final TemporalUtils temporalUtils;

  public Optional<CreditTransaction> grantIncludedCredits(String userId, SubscriptionProduct plan) {
    var includedCredits = plan.includedCreditsPerBillingPeriodOrDefault();
    if (includedCredits <= 0) {
      log.info(
          "SubscriptionProduct(id={}) includes no credit, nothing granted to User(id={})",
          plan.getId(),
          userId);
      return Optional.empty();
    }
    if (alreadyGrantedThisBillingPeriod(userId)) {
      log.info(
          "User(id={}) was already granted the credits included in its billing period, skipping",
          userId);
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
                .expirationDatetime(temporalUtils.startOfNextMonthInstant())
                .build());
    log.info(
        "Granted {} credits to User(id={}) from SubscriptionProduct(id={}),"
            + " CreditTransaction.id={}",
        includedCredits,
        userId,
        plan.getId(),
        granted.getId());
    return Optional.of(granted);
  }

  private boolean alreadyGrantedThisBillingPeriod(String userId) {
    return creditTransactionRepository.existsByUserIdAndTypeAndCreationDatetimeGreaterThanEqual(
        userId, SUBSCRIPTION_GRANT, temporalUtils.startOfMonth());
  }

  private String grantLabel(SubscriptionProduct plan) {
    return plan.getName() == null
        ? DEFAULT_GRANT_LABEL
        : DEFAULT_GRANT_LABEL + " " + plan.getName();
  }
}
