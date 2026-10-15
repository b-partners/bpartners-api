package app.bpartners.api.service.subscription;

import static app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.ABANDONED;
import static app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.AWAITING_PAYMENT;
import static app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.EXPIRED;
import static app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.PENDING;
import static app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.PROCESSING;

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

  public enum Abandonment {
    ABANDONED,
    PAID,
    NOT_ABANDONABLE
  }

  @Transactional
  public Optional<UserSubscriptionUpgrade> confirm(String upgradeId) {
    upgradeRepository.acquireLock(upgradeId);
    return upgradeRepository
        .findById(upgradeId)
        .filter(upgrade -> upgrade.getStatus() == PENDING)
        .filter(upgrade -> !isQuoteOverdue(upgrade))
        .map(
            upgrade ->
                upgradeRepository.save(
                    upgrade.toBuilder().confirmationDatetime(Instant.now()).status(PROCESSING).build()));
  }

  @Transactional
  public boolean expireQuote(String upgradeId) {
    upgradeRepository.acquireLock(upgradeId);
    var upgrade = upgradeRepository.findById(upgradeId).orElse(null);
    if (upgrade == null || upgrade.getStatus() != PENDING || !isQuoteOverdue(upgrade)) {
      return false;
    }
    upgradeRepository.save(upgrade.toBuilder().status(EXPIRED).build());
    return true;
  }

  @Transactional
  public Optional<UserSubscriptionUpgrade> claimPaid(String upgradeId, String stripeInvoiceId) {
    upgradeRepository.acquireLock(upgradeId);
    return upgradeRepository
        .findById(upgradeId)
        .filter(upgrade -> upgrade.getStatus() == AWAITING_PAYMENT)
        .filter(upgrade -> stripeInvoiceId.equals(upgrade.getStripeProrationInvoiceId()))
        .map(upgrade -> upgradeRepository.save(upgrade.toBuilder().status(PROCESSING).build()));
  }

  @Transactional
  public Expiration expire(String upgradeId) {
    upgradeRepository.acquireLock(upgradeId);
    var upgrade = upgradeRepository.findById(upgradeId).orElse(null);
    if (upgrade == null || upgrade.getStatus() != AWAITING_PAYMENT || !isPaymentOverdue(upgrade)) {
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

  @Transactional
  public Abandonment abandon(String upgradeId) {
    upgradeRepository.acquireLock(upgradeId);
    var upgrade = upgradeRepository.findById(upgradeId).orElse(null);
    if (upgrade == null) {
      return Abandonment.NOT_ABANDONABLE;
    }
    if (upgrade.getStatus() == PENDING && !isQuoteOverdue(upgrade)) {
      upgradeRepository.save(upgrade.toBuilder().status(ABANDONED).build());
      return Abandonment.ABANDONED;
    }
    if (upgrade.getStatus() != AWAITING_PAYMENT || isPaymentOverdue(upgrade)) {
      return Abandonment.NOT_ABANDONABLE;
    }
    return switch (stripeUpgradeService.voidOpenInvoice(upgrade.getStripeProrationInvoiceId())) {
      case VOIDED -> {
        upgradeRepository.save(upgrade.toBuilder().status(ABANDONED).build());
        yield Abandonment.ABANDONED;
      }
      case PAID -> {
        log.info(
            "Stripe Invoice(id={}) of SubscriptionUpgrade(id={}) was paid before its abandonment,"
                + " not abandoning it",
            upgrade.getStripeProrationInvoiceId(),
            upgrade.getId());
        yield Abandonment.PAID;
      }
    };
  }

  static boolean isPaymentOverdue(UserSubscriptionUpgrade upgrade) {
    var expiration = upgrade.getPaymentExpirationDatetime();
    return expiration != null && expiration.isBefore(Instant.now());
  }

  static boolean isQuoteOverdue(UserSubscriptionUpgrade upgrade) {
    var expiration = upgrade.getQuoteExpirationDatetime();
    return expiration != null && expiration.isBefore(Instant.now());
  }
}
