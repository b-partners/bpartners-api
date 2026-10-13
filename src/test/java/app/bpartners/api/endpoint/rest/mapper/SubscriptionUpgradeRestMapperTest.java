package app.bpartners.api.endpoint.rest.mapper;

import static app.bpartners.api.model.subscription.BillingInterval.MONTHLY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import app.bpartners.api.endpoint.rest.model.SubscriptionUpgradeStatus;
import app.bpartners.api.model.subscription.UserSubscriptionUpgrade;
import org.junit.jupiter.api.Test;

class SubscriptionUpgradeRestMapperTest {
  SubscriptionUpgradeRestMapper subject = new SubscriptionUpgradeRestMapper();

  @Test
  void maps_the_latest_status_of_the_upgrade() {
    var upgrade =
        UserSubscriptionUpgrade.builder()
            .id("upgrade_id")
            .fromBillingInterval(MONTHLY)
            .toBillingInterval(MONTHLY)
            .status(app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.PENDING)
            .build();

    assertEquals(SubscriptionUpgradeStatus.PENDING, subject.toRest(upgrade).getStatus());
  }

  @Test
  void maps_an_upgrade_without_status_history_to_no_status() {
    var upgrade =
        UserSubscriptionUpgrade.builder()
            .id("upgrade_id")
            .fromBillingInterval(MONTHLY)
            .toBillingInterval(MONTHLY)
            .build();

    assertNull(subject.toRest(upgrade).getStatus());
  }

  @Test
  void maps_a_rest_status_to_the_domain_one_keeping_the_absence_of_filter() {
    assertEquals(
        app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.PAID,
        SubscriptionUpgradeRestMapper.statusToDomain(SubscriptionUpgradeStatus.PAID));
    assertNull(SubscriptionUpgradeRestMapper.statusToDomain(null));
  }
}
