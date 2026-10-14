package app.bpartners.api.endpoint.rest.mapper;

import static app.bpartners.api.model.subscription.BillingInterval.MONTHLY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import app.bpartners.api.endpoint.rest.model.SubscriptionUpgradeStatus;
import app.bpartners.api.model.subscription.UserSubscriptionUpgrade;
import java.time.Instant;
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
  void exposes_the_payment_page_and_its_deadline_while_awaiting_the_payment() {
    var deadline = Instant.parse("2026-10-14T07:00:00Z");
    var upgrade =
        UserSubscriptionUpgrade.builder()
            .id("upgrade_id")
            .fromBillingInterval(MONTHLY)
            .toBillingInterval(MONTHLY)
            .paymentUrl("https://pay.example/in_open")
            .paymentExpirationDatetime(deadline)
            .status(app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.AWAITING_PAYMENT)
            .build();

    var actual = subject.toRest(upgrade);

    assertEquals(SubscriptionUpgradeStatus.AWAITING_PAYMENT, actual.getStatus());
    assertEquals("https://pay.example/in_open", actual.getPaymentUrl());
    assertEquals(deadline, actual.getPaymentExpirationDatetime());
  }

  @Test
  void hides_the_payment_page_once_the_upgrade_is_no_longer_awaiting_the_payment() {
    var upgrade =
        UserSubscriptionUpgrade.builder()
            .id("upgrade_id")
            .fromBillingInterval(MONTHLY)
            .toBillingInterval(MONTHLY)
            .paymentUrl("https://pay.example/in_open")
            .paymentExpirationDatetime(Instant.parse("2026-10-14T07:00:00Z"))
            .status(app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.PAID)
            .build();

    var actual = subject.toRest(upgrade);

    assertNull(actual.getPaymentUrl());
    assertNull(actual.getPaymentExpirationDatetime());
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
