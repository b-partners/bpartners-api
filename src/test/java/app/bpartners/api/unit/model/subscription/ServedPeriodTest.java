package app.bpartners.api.unit.model.subscription;

import static app.bpartners.api.model.subscription.BillingInterval.MONTHLY;
import static app.bpartners.api.model.subscription.BillingInterval.YEARLY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import app.bpartners.api.model.subscription.ServedPeriod;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ServedPeriodTest {
  private static final Instant POST_PAID_PERIOD_START = Instant.parse("2026-09-04T21:00:00Z");
  private static final Instant POST_PAID_PERIOD_END = Instant.parse("2026-10-04T21:00:00Z");
  private static final Instant PRE_PAID_PERIOD_START = Instant.parse("2026-10-04T21:00:00Z");
  private static final Instant PRE_PAID_PERIOD_END = Instant.parse("2026-11-04T22:00:00Z");

  @Test
  void running_subscription_keeps_stripe_period() {
    var actual = ServedPeriod.of(POST_PAID_PERIOD_START, POST_PAID_PERIOD_END, null, MONTHLY);

    assertEquals(new ServedPeriod(POST_PAID_PERIOD_START, POST_PAID_PERIOD_END), actual);
  }

  @Test
  void terminated_post_paid_subscription_reports_previous_monthly_period() {
    var actual =
        ServedPeriod.of(
            POST_PAID_PERIOD_START,
            POST_PAID_PERIOD_END,
            Instant.parse("2026-09-04T22:00:41Z"),
            MONTHLY);

    assertEquals(
        new ServedPeriod(Instant.parse("2026-08-04T21:00:00Z"), POST_PAID_PERIOD_START), actual);
  }

  @Test
  void terminated_post_paid_subscription_reports_previous_yearly_period() {
    var actual =
        ServedPeriod.of(
            POST_PAID_PERIOD_START,
            Instant.parse("2027-09-04T21:00:00Z"),
            Instant.parse("2026-09-04T22:00:41Z"),
            YEARLY);

    assertEquals(
        new ServedPeriod(Instant.parse("2025-09-04T21:00:00Z"), POST_PAID_PERIOD_START), actual);
  }

  @Test
  void terminated_pre_paid_subscription_ends_at_termination() {
    var endedAt = Instant.parse("2026-10-20T09:30:00Z");

    var actual = ServedPeriod.of(PRE_PAID_PERIOD_START, PRE_PAID_PERIOD_END, endedAt, MONTHLY);

    assertEquals(new ServedPeriod(PRE_PAID_PERIOD_START, endedAt), actual);
  }

  @Test
  void terminated_pre_paid_subscription_never_outlives_its_period() {
    var actual =
        ServedPeriod.of(
            PRE_PAID_PERIOD_START,
            PRE_PAID_PERIOD_END,
            Instant.parse("2026-12-01T00:00:00Z"),
            MONTHLY);

    assertEquals(new ServedPeriod(PRE_PAID_PERIOD_START, PRE_PAID_PERIOD_END), actual);
  }

  @Test
  void terminated_pre_paid_subscription_without_period_end_ends_at_termination() {
    var endedAt = Instant.parse("2026-10-20T09:30:00Z");

    var actual = ServedPeriod.of(PRE_PAID_PERIOD_START, null, endedAt, MONTHLY);

    assertEquals(new ServedPeriod(PRE_PAID_PERIOD_START, endedAt), actual);
  }

  @Test
  void terminated_subscription_without_period_start_ends_at_termination() {
    var endedAt = Instant.parse("2026-10-20T09:30:00Z");

    var actual = ServedPeriod.of(null, PRE_PAID_PERIOD_END, endedAt, MONTHLY);

    assertNull(actual.start());
    assertEquals(endedAt, actual.end());
  }
}
