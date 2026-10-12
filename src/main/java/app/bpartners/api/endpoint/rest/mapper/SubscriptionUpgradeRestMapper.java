package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.BillingInterval;
import app.bpartners.api.endpoint.rest.model.SubscriptionUpgrade;
import app.bpartners.api.endpoint.rest.model.SubscriptionUpgradeStatus;
import app.bpartners.api.model.subscription.UserSubscriptionUpgrade;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionUpgradeRestMapper {

  public SubscriptionUpgrade toRest(UserSubscriptionUpgrade upgrade) {
    return new SubscriptionUpgrade()
        .id(upgrade.getId())
        .fromSubscriptionPlanIdentifier(upgrade.getFromSubscriptionProductId())
        .toSubscriptionPlanIdentifier(upgrade.getToSubscriptionProductId())
        .fromBillingInterval(billingIntervalToRest(upgrade.getFromBillingInterval()))
        .toBillingInterval(billingIntervalToRest(upgrade.getToBillingInterval()))
        .upgradeDatetime(upgrade.getUpgradeDatetime())
        .newCommitmentEnd(upgrade.getNewCommitmentEndDatetime())
        .prorationAmountInCentsWithVat(upgrade.getProrationAmountInCentsWithVat())
        .status(SubscriptionUpgradeStatus.valueOf(upgrade.getStatus().name()));
  }

  public static app.bpartners.api.model.subscription.SubscriptionUpgradeStatus statusToDomain(
      SubscriptionUpgradeStatus status) {
    return status == null
        ? null
        : app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.valueOf(status.name());
  }

  private static BillingInterval billingIntervalToRest(
      app.bpartners.api.model.subscription.BillingInterval billingInterval) {
    return switch (billingInterval) {
      case MONTHLY -> BillingInterval.MONTHLY;
      case YEARLY -> BillingInterval.YEARLY;
    };
  }
}
