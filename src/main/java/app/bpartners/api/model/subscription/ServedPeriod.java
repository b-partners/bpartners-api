package app.bpartners.api.model.subscription;

import static java.time.ZoneOffset.UTC;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public record ServedPeriod(Instant start, Instant end) {
  public static final Instant POST_PAID_ACCOUNTING_UNTIL =
      LocalDate.of(2026, 9, 5).atStartOfDay(ZoneId.of("Europe/Paris")).toInstant();

  public static ServedPeriod of(
      Instant currentPeriodStart,
      Instant currentPeriodEnd,
      Instant endedAt,
      BillingInterval billingInterval) {
    if (endedAt == null) {
      return new ServedPeriod(currentPeriodStart, currentPeriodEnd);
    }
    if (currentPeriodStart == null) {
      return new ServedPeriod(null, endedAt);
    }
    if (isBilledAsPostPaid(currentPeriodStart)) {
      return new ServedPeriod(
          previousPeriodStartOf(currentPeriodStart, billingInterval), currentPeriodStart);
    }
    return new ServedPeriod(currentPeriodStart, earliestOf(endedAt, currentPeriodEnd));
  }

  private static boolean isBilledAsPostPaid(Instant currentPeriodStart) {
    return currentPeriodStart.isBefore(POST_PAID_ACCOUNTING_UNTIL);
  }

  private static Instant previousPeriodStartOf(
      Instant currentPeriodStart, BillingInterval billingInterval) {
    var utcPeriodStart = currentPeriodStart.atZone(UTC);
    return BillingInterval.YEARLY.equals(billingInterval)
        ? utcPeriodStart.minusYears(1L).toInstant()
        : utcPeriodStart.minusMonths(1L).toInstant();
  }

  private static Instant earliestOf(Instant endedAt, Instant currentPeriodEnd) {
    return currentPeriodEnd == null || endedAt.isBefore(currentPeriodEnd)
        ? endedAt
        : currentPeriodEnd;
  }
}
