package app.bpartners.api.service.subscription;

import static app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.AWAITING_PAYMENT;
import static app.bpartners.api.service.subscription.SubscriptionUpgradeTransitionService.isPaymentOverdue;
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
    log.info("Expired {} SubscriptionUpgrade(s) whose payment deadline is over", expired);
    return (int) expired;
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
