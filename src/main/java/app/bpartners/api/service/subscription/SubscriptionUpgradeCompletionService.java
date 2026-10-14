package app.bpartners.api.service.subscription;

import static app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.PAID;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.SubscriptionUpgradeInvoicingRequested;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.model.subscription.UserSubscriptionUpgrade;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.jpa.SubscriptionProductRepository;
import app.bpartners.api.repository.jpa.UserSubscriptionUpgradeJpaRepository;
import app.bpartners.api.service.credit.CreditGrantService;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionUpgradeCompletionService {
  private final SubscriptionProductRepository subscriptionProductRepository;
  private final UserRepository userRepository;
  private final StripeUpgradeService stripeUpgradeService;
  private final SubscriptionService subscriptionService;
  private final SubscriptionUpgradeCommitmentService commitmentService;
  private final SubscriptionUpgradeTransitionService transitionService;
  private final UserSubscriptionUpgradeJpaRepository upgradeRepository;
  private final SubscriptionUpgradeTimeline timeline;
  private final CreditGrantService creditGrantService;
  private final EventProducer eventProducer;

  public UserSubscriptionUpgrade complete(
      User user, UserSubscriptionUpgrade upgrade, String paymentMethodId) {
    var toProduct = toProductOf(upgrade);
    var toInterval = upgrade.getToBillingInterval();
    var withSubscription =
        upgradeRepository.save(
            upgrade.toBuilder()
                .newStripeSubscriptionId(
                    stripeUpgradeService.createUpgradedSubscription(
                        user.getUserSubscriptionId(),
                        paymentMethodId,
                        toProduct,
                        toInterval,
                        timeline.billingCycleAnchorOf(upgrade).getEpochSecond(),
                        upgrade.getId()))
                .build());
    subscriptionService.cancelSubscriptionImmediately(upgrade.getOldStripeSubscriptionId());

    commitmentService.switchTo(
        user.getId(),
        toProduct.getId(),
        toInterval,
        upgrade.getUpgradeDatetime(),
        upgrade.getNewCommitmentEndDatetime());
    var switched = upgradeRepository.save(withSubscription.toBuilder().status(PAID).build());
    grantCredits(user, toProduct);
    eventProducer.accept(
        List.of(
            SubscriptionUpgradeInvoicingRequested.builder()
                .subscriptionUpgradeId(switched.getId())
                .build()));
    log.info(
        "User(id={}) upgraded from SubscriptionProduct(id={}, {}) to SubscriptionProduct(id={},"
            + " {}), {} cents charged",
        user.getId(),
        upgrade.getFromSubscriptionProductId(),
        upgrade.getFromBillingInterval(),
        toProduct.getId(),
        toInterval,
        upgrade.getProrationAmountInCentsWithVat());
    return switched;
  }

  public Optional<UserSubscriptionUpgrade> completeAfterPayment(
      String upgradeId, String stripeInvoiceId) {
    var claimed = transitionService.claimPaid(upgradeId, stripeInvoiceId);
    if (claimed.isEmpty()) {
      log.info(
          "Stripe Invoice(id={}) paid but SubscriptionUpgrade(id={}) is not awaiting it, skipping",
          stripeInvoiceId,
          upgradeId);
      return Optional.empty();
    }
    var upgrade = claimed.get();
    try {
      var user = userRepository.getById(upgrade.getUserId());
      var paymentMethodId =
          stripeUpgradeService.resolveChargedPaymentMethod(
              user.getUserSubscriptionId(), stripeInvoiceId);
      return Optional.of(complete(user, upgrade, paymentMethodId));
    } catch (RuntimeException e) {
      log.error(
          "SubscriptionUpgrade(id={}) was paid through Stripe Invoice(id={}) but could not be"
              + " completed, it stays PENDING for a manual recovery",
          upgradeId,
          stripeInvoiceId,
          e);
      return Optional.empty();
    }
  }

  private SubscriptionProduct toProductOf(UserSubscriptionUpgrade upgrade) {
    return subscriptionProductRepository
        .findById(upgrade.getToSubscriptionProductId())
        .orElseThrow(
            () ->
                new NotFoundException(
                    "SubscriptionProduct(id="
                        + upgrade.getToSubscriptionProductId()
                        + ") not found"));
  }

  private void grantCredits(User user, SubscriptionProduct toProduct) {
    try {
      creditGrantService.grantIncludedCredits(user.getId(), toProduct);
    } catch (RuntimeException e) {
      log.error(
          "Upgrade of User(id={}) done but the credits of SubscriptionProduct(id={}) were not"
              + " granted",
          user.getId(),
          toProduct.getId(),
          e);
    }
  }
}
