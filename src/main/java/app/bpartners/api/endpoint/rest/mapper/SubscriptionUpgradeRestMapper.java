package app.bpartners.api.endpoint.rest.mapper;

import static app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.AWAITING_PAYMENT;
import static app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.PENDING;

import app.bpartners.api.endpoint.rest.model.BillingInterval;
import app.bpartners.api.endpoint.rest.model.SubscriptionUpgrade;
import app.bpartners.api.endpoint.rest.model.SubscriptionUpgradeCredit;
import app.bpartners.api.endpoint.rest.model.SubscriptionUpgradeLine;
import app.bpartners.api.endpoint.rest.model.SubscriptionUpgradeLineKind;
import app.bpartners.api.endpoint.rest.model.SubscriptionUpgradeStatus;
import app.bpartners.api.model.subscription.UpgradeLineKind;
import app.bpartners.api.model.subscription.UserSubscriptionUpgrade;
import app.bpartners.api.model.subscription.UserSubscriptionUpgradeLine;
import java.util.Comparator;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionUpgradeRestMapper {

  public SubscriptionUpgrade toRest(UserSubscriptionUpgrade upgrade) {
    var awaitingPayment = upgrade.getStatus() == AWAITING_PAYMENT;
    var pending = upgrade.getStatus() == PENDING;
    return new SubscriptionUpgrade()
        .id(upgrade.getId())
        .fromSubscriptionPlanIdentifier(upgrade.getFromSubscriptionProductId())
        .toSubscriptionPlanIdentifier(upgrade.getToSubscriptionProductId())
        .fromBillingInterval(billingIntervalToRest(upgrade.getFromBillingInterval()))
        .toBillingInterval(billingIntervalToRest(upgrade.getToBillingInterval()))
        .upgradeDatetime(upgrade.getUpgradeDatetime())
        .newCommitmentEnd(upgrade.getNewCommitmentEndDatetime())
        .prorationAmountInCentsWithVat(upgrade.getProrationAmountInCentsWithVat())
        .newChargeInCentsWithVat(upgrade.getNewChargeInCentsWithVat())
        .creditedInCentsWithVat(upgrade.getCreditedInCentsWithVat())
        .currentSubscriptionCredit(creditToRest(upgrade))
        .lines(upgrade.getLines().stream().map(this::lineToRest).toList())
        .status(statusToRest(upgrade.getStatus()))
        .quoteExpirationDatetime(pending ? upgrade.getQuoteExpirationDatetime() : null)
        .paymentUrl(awaitingPayment ? upgrade.getPaymentUrl() : null)
        .paymentExpirationDatetime(awaitingPayment ? upgrade.getPaymentExpirationDatetime() : null);
  }

  private SubscriptionUpgradeCredit creditToRest(UserSubscriptionUpgrade upgrade) {
    var creditLines =
        upgrade.getLines().stream().filter(line -> line.getKind() == UpgradeLineKind.CREDIT).toList();
    if (upgrade.getPaidAmountInCentsWithVat() == null
        || upgrade.getCreditedInCentsWithVat() == null
        || creditLines.isEmpty()) {
      return null;
    }
    var remaining = upgrade.getCreditedInCentsWithVat();
    return new SubscriptionUpgradeCredit()
        .paidPeriodStart(upgrade.getPaidPeriodStartDate())
        .paidPeriodEnd(upgrade.getPaidPeriodEndDate())
        .paidAmountInCentsWithVat(upgrade.getPaidAmountInCentsWithVat())
        .consumedAmountInCentsWithVat(upgrade.getPaidAmountInCentsWithVat() - remaining)
        .remainingPeriodStart(
            creditLines.stream()
                .map(UserSubscriptionUpgradeLine::getPeriodStartDate)
                .min(Comparator.naturalOrder())
                .orElseThrow())
        .remainingPeriodEnd(
            creditLines.stream()
                .map(UserSubscriptionUpgradeLine::getPeriodEndDate)
                .max(Comparator.naturalOrder())
                .orElseThrow())
        .remainingAmountInCentsWithVat(remaining);
  }

  private SubscriptionUpgradeLine lineToRest(UserSubscriptionUpgradeLine line) {
    return new SubscriptionUpgradeLine()
        .kind(SubscriptionUpgradeLineKind.valueOf(line.getKind().name()))
        .subscriptionPlanIdentifier(line.getSubscriptionProductId())
        .billingInterval(billingIntervalToRest(line.getBillingInterval()))
        .periodStart(line.getPeriodStartDate())
        .periodEnd(line.getPeriodEndDate())
        .days(line.getDays())
        .referencePeriodDays(line.getReferencePeriodDays())
        .referencePriceInCentsWithVat(line.getReferencePriceInCentsWithVat())
        .amountInCentsWithVat(line.getAmountInCentsWithVat());
  }

  private static SubscriptionUpgradeStatus statusToRest(
      app.bpartners.api.model.subscription.SubscriptionUpgradeStatus status) {
    return status == null ? null : SubscriptionUpgradeStatus.valueOf(status.name());
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
