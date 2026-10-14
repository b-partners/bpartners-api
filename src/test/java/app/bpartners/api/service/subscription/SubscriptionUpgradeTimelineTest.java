package app.bpartners.api.service.subscription;

import static app.bpartners.api.model.subscription.BillingInterval.MONTHLY;
import static app.bpartners.api.model.subscription.BillingInterval.YEARLY;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.api.model.subscription.BillingInterval;
import app.bpartners.api.model.subscription.UserSubscriptionUpgrade;
import app.bpartners.api.service.utils.TemporalUtils;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class SubscriptionUpgradeTimelineTest {
  private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

  SubscriptionUpgradeTimeline subject = new SubscriptionUpgradeTimeline(new TemporalUtils());

  @Test
  void keeps_the_payment_window_of_48_hours_and_the_margin_of_60_minutes() {
    assertEquals(Duration.ofHours(48), SubscriptionUpgradeTimeline.PAYMENT_WINDOW);
    assertEquals(Duration.ofMinutes(60), SubscriptionUpgradeTimeline.PAYMENT_MARGIN);
  }

  @Test
  void anchors_a_monthly_upgrade_on_the_first_day_of_the_next_month() {
    var upgrade = upgrade("2026-10-12", "2026-10-31", MONTHLY, "2026-10-12T08:00:00Z");

    assertEquals(
        LocalDate.of(2026, 11, 1).atStartOfDay(PARIS).toInstant(),
        subject.billingCycleAnchorOf(upgrade));
  }

  @Test
  void anchors_a_yearly_upgrade_a_year_after_the_upgrade_date() {
    var upgrade = upgrade("2026-10-12", "2026-10-31", YEARLY, "2026-10-12T08:00:00Z");

    assertEquals(
        LocalDate.of(2027, 10, 12).atStartOfDay(PARIS).toInstant(),
        subject.billingCycleAnchorOf(upgrade));
  }

  @Test
  void expires_48_hours_after_the_creation_minus_the_margin_when_nothing_comes_sooner() {
    var upgrade = upgrade("2026-10-12", "2026-10-31", MONTHLY, "2026-10-12T08:00:00Z");

    assertEquals(Instant.parse("2026-10-14T07:00:00Z"), subject.paymentExpirationOf(upgrade));
  }

  @Test
  void expires_before_the_anchor_of_the_new_subscription_minus_the_margin() {
    var upgrade = upgrade("2026-10-31", "2026-10-31", MONTHLY, "2026-10-31T14:00:00Z");

    assertEquals(Instant.parse("2026-10-31T22:00:00Z"), subject.paymentExpirationOf(upgrade));
  }

  @Test
  void expires_before_the_renewal_of_the_old_subscription_minus_the_margin() {
    var upgrade = upgrade("2026-10-12", "2026-10-13", YEARLY, "2026-10-12T08:00:00Z");

    assertEquals(Instant.parse("2026-10-13T21:00:00Z"), subject.paymentExpirationOf(upgrade));
  }

  @Test
  void never_expires_before_the_creation_of_the_upgrade() {
    var upgrade = upgrade("2026-10-31", "2026-10-31", MONTHLY, "2026-10-31T22:30:00Z");

    assertEquals(Instant.parse("2026-10-31T22:30:00Z"), subject.paymentExpirationOf(upgrade));
  }

  private static UserSubscriptionUpgrade upgrade(
      String upgradeDate, String oldCoverageEnd, BillingInterval toInterval, String creation) {
    return UserSubscriptionUpgrade.builder()
        .id("upgrade_id")
        .upgradeDatetime(LocalDate.parse(upgradeDate).atStartOfDay(PARIS).toInstant())
        .oldCoverageEndDatetime(LocalDate.parse(oldCoverageEnd).atStartOfDay(PARIS).toInstant())
        .toBillingInterval(toInterval)
        .creationDatetime(Instant.parse(creation))
        .build();
  }
}
