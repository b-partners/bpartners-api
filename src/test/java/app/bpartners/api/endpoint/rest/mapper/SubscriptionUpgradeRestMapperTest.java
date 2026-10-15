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

  @Test
  void exposes_the_proration_detail_and_the_quote_deadline_while_pending() {
    var deadline = Instant.parse("2026-10-12T22:00:00Z");
    var upgrade =
        UserSubscriptionUpgrade.builder()
            .id("upgrade_id")
            .fromBillingInterval(MONTHLY)
            .toBillingInterval(MONTHLY)
            .prorationAmountInCentsWithVat(3_871L)
            .newChargeInCentsWithVat(7_742L)
            .creditedInCentsWithVat(3_871L)
            .paidPeriodStartDate(java.time.LocalDate.of(2026, 10, 1))
            .paidPeriodEndDate(java.time.LocalDate.of(2026, 10, 31))
            .paidAmountInCentsWithVat(6_000L)
            .quoteExpirationDatetime(deadline)
            .lines(
                java.util.List.of(
                    app.bpartners.api.model.subscription.UserSubscriptionUpgradeLine.builder()
                        .id("line_id")
                        .lineOrder(0)
                        .kind(app.bpartners.api.model.subscription.UpgradeLineKind.NEW_CHARGE)
                        .subscriptionProductId("pro_id")
                        .billingInterval(MONTHLY)
                        .periodStartDate(java.time.LocalDate.of(2026, 10, 12))
                        .periodEndDate(java.time.LocalDate.of(2026, 10, 31))
                        .days(20)
                        .referencePeriodDays(31)
                        .referencePriceInCentsWithVat(12_000L)
                        .amountInCentsWithVat(7_742L)
                        .build(),
                    app.bpartners.api.model.subscription.UserSubscriptionUpgradeLine.builder()
                        .id("credit_line_id")
                        .lineOrder(1)
                        .kind(app.bpartners.api.model.subscription.UpgradeLineKind.CREDIT)
                        .subscriptionProductId("essential_id")
                        .billingInterval(MONTHLY)
                        .periodStartDate(java.time.LocalDate.of(2026, 10, 12))
                        .periodEndDate(java.time.LocalDate.of(2026, 10, 31))
                        .days(20)
                        .referencePeriodDays(31)
                        .referencePriceInCentsWithVat(6_000L)
                        .amountInCentsWithVat(3_871L)
                        .build()))
            .status(app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.PENDING)
            .build();

    var actual = subject.toRest(upgrade);

    assertEquals(deadline, actual.getQuoteExpirationDatetime());
    assertEquals(7_742L, actual.getNewChargeInCentsWithVat());
    assertEquals(3_871L, actual.getCreditedInCentsWithVat());
    var line = actual.getLines().getFirst();
    assertEquals(
        app.bpartners.api.endpoint.rest.model.SubscriptionUpgradeLineKind.NEW_CHARGE,
        line.getKind());
    assertEquals("pro_id", line.getSubscriptionPlanIdentifier());
    assertEquals(20, line.getDays());
    assertEquals(31, line.getReferencePeriodDays());
    assertEquals(12_000L, line.getReferencePriceInCentsWithVat());
    assertEquals(7_742L, line.getAmountInCentsWithVat());
    var credit = actual.getCurrentSubscriptionCredit();
    assertEquals(java.time.LocalDate.of(2026, 10, 1), credit.getPaidPeriodStart());
    assertEquals(java.time.LocalDate.of(2026, 10, 31), credit.getPaidPeriodEnd());
    assertEquals(6_000L, credit.getPaidAmountInCentsWithVat());
    assertEquals(2_129L, credit.getConsumedAmountInCentsWithVat());
    assertEquals(java.time.LocalDate.of(2026, 10, 12), credit.getRemainingPeriodStart());
    assertEquals(java.time.LocalDate.of(2026, 10, 31), credit.getRemainingPeriodEnd());
    assertEquals(3_871L, credit.getRemainingAmountInCentsWithVat());
  }

  @Test
  void has_no_credit_detail_for_an_upgrade_started_before_it_was_kept() {
    var upgrade =
        UserSubscriptionUpgrade.builder()
            .id("upgrade_id")
            .fromBillingInterval(MONTHLY)
            .toBillingInterval(MONTHLY)
            .status(app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.PAID)
            .build();

    assertNull(subject.toRest(upgrade).getCurrentSubscriptionCredit());
  }

  @Test
  void hides_the_quote_deadline_once_the_upgrade_is_no_longer_pending() {
    var upgrade =
        UserSubscriptionUpgrade.builder()
            .id("upgrade_id")
            .fromBillingInterval(MONTHLY)
            .toBillingInterval(MONTHLY)
            .quoteExpirationDatetime(Instant.parse("2026-10-12T22:00:00Z"))
            .status(app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.PROCESSING)
            .build();

    assertNull(subject.toRest(upgrade).getQuoteExpirationDatetime());
  }
}
