package app.bpartners.api.service.subscription;

import static app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.AWAITING_PAYMENT;
import static app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.EXPIRED;
import static app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.PENDING;

import app.bpartners.api.model.subscription.UserSubscriptionUpgrade;
import app.bpartners.api.repository.jpa.UserSubscriptionUpgradeJpaRepository;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionUpgradeTransitionService {
  private final UserSubscriptionUpgradeJpaRepository upgradeRepository;
  private final StripeUpgradeService stripeUpgradeService;

  public enum Expiration {
    EXPIRED,
    PAID,
    SKIPPED
  }

  @Transactional
  public Optional<UserSubscriptionUpgrade> claimPaid(String upgradeId, String stripeInvoiceId) {
    upgradeRepository.acquireLock(upgradeId);
    return upgradeRepository
        .findById(upgradeId)
        .filter(upgrade -> upgrade.getStatus() == AWAITING_PAYMENT)
        .filter(upgrade -> stripeInvoiceId.equals(upgrade.getStripeProrationInvoiceId()))
        .map(upgrade -> upgradeRepository.save(upgrade.toBuilder().status(PENDING).build()));
  }

  @Transactional
  public Expiration expire(String upgradeId) {
    upgradeRepository.acquireLock(upgradeId);
    var upgrade = upgradeRepository.findById(upgradeId).orElse(null);
    if (upgrade == null
        || upgrade.getStatus() != AWAITING_PAYMENT
        || !isPaymentOverdue(upgrade)) {
      return Expiration.SKIPPED;
    }
    return switch (stripeUpgradeService.voidOpenInvoice(upgrade.getStripeProrationInvoiceId())) {
      case VOIDED -> {
        upgradeRepository.save(upgrade.toBuilder().status(EXPIRED).build());
        yield Expiration.EXPIRED;
      }
      case PAID -> {
        log.info(
            "Stripe Invoice(id={}) of SubscriptionUpgrade(id={}) was paid at the deadline, not"
                + " expiring it",
            upgrade.getStripeProrationInvoiceId(),
            upgrade.getId());
        yield Expiration.PAID;
      }
    };
  }

  static boolean isPaymentOverdue(UserSubscriptionUpgrade upgrade) {
    var expiration = upgrade.getPaymentExpirationDatetime();
    return expiration != null && expiration.isBefore(Instant.now());
  }
}
