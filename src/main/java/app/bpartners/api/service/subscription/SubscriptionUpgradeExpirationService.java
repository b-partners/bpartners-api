package app.bpartners.api.service.subscription;

import static app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.AWAITING_PAYMENT;
import static app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.PENDING;
import static app.bpartners.api.service.subscription.SubscriptionUpgradeTransitionService.isPaymentOverdue;
import static app.bpartners.api.service.subscription.SubscriptionUpgradeTransitionService.isQuoteOverdue;
import static java.time.Instant.now;

import app.bpartners.api.model.subscription.UserSubscriptionUpgrade;
import app.bpartners.api.repository.jpa.UserSubscriptionUpgradeJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionUpgradeExpirationService {
  private final UserSubscriptionUpgradeJpaRepository upgradeRepository;
  private final SubscriptionUpgradeTransitionService transitionService;
  private final SubscriptionUpgradeCompletionService completionService;

  public int expireOverdue() {
    var expired =
        upgradeRepository.findAllByPaymentExpirationDatetimeBefore(now()).stream()
            .filter(this::expireIfOverdue)
            .count();
    var expiredQuotes =
        upgradeRepository.findAllByQuoteExpirationDatetimeBefore(now()).stream()
            .filter(this::expireQuoteIfOverdue)
            .count();
    log.info(
        "Expired {} SubscriptionUpgrade(s) whose payment deadline is over and {} whose quote is"
            + " over",
        expired,
        expiredQuotes);
    return (int) (expired + expiredQuotes);
  }

  public boolean expireQuoteIfOverdue(UserSubscriptionUpgrade upgrade) {
    if (upgrade.getStatus() != PENDING || !isQuoteOverdue(upgrade)) {
      return false;
    }
    try {
      var expired = transitionService.expireQuote(upgrade.getId());
      if (expired) {
        log.info("SubscriptionUpgrade(id={}) expired, its quote is over", upgrade.getId());
      }
      return expired;
    } catch (RuntimeException e) {
      log.warn("Unable to expire the quote of SubscriptionUpgrade(id={})", upgrade.getId(), e);
      return false;
    }
  }

  public boolean expireIfOverdue(UserSubscriptionUpgrade upgrade) {
    if (upgrade.getStatus() != AWAITING_PAYMENT || !isPaymentOverdue(upgrade)) {
      return false;
    }
    try {
      return switch (transitionService.expire(upgrade.getId())) {
        case EXPIRED -> {
          log.info(
              "SubscriptionUpgrade(id={}) expired, its payment deadline is over", upgrade.getId());
          yield true;
        }
        case PAID -> {
          completionService.completeAfterPayment(
              upgrade.getId(), upgrade.getStripeProrationInvoiceId());
          yield false;
        }
        case SKIPPED -> false;
      };
    } catch (RuntimeException e) {
      log.warn("Unable to expire SubscriptionUpgrade(id={})", upgrade.getId(), e);
      return false;
    }
  }
}
