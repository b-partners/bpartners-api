package app.bpartners.api.service.subscription;

import app.bpartners.api.model.UserSubscriptionProduct;
import app.bpartners.api.model.subscription.BillingInterval;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;

record SubscriptionFlows(
    long baselineMonthly,
    long baselineAnnual,
    long[] newMonthly,
    long[] newAnnual,
    long[] expiredMonthly,
    long[] expiredAnnual) {

  static SubscriptionFlows aggregate(
      List<UserSubscriptionProduct> subs,
      Instant fromInstant,
      Instant toInstant,
      int bucketCount,
      ToIntFunction<Instant> bucketIndex) {
    long baselineMonthly = 0;
    long baselineAnnual = 0;
    long[] newMonthly = new long[bucketCount];
    long[] newAnnual = new long[bucketCount];
    long[] expiredMonthly = new long[bucketCount];
    long[] expiredAnnual = new long[bucketCount];
    for (UserSubscriptionProduct usp : subs) {
      var interval = usp.getBillingInterval();
      boolean monthly = interval == BillingInterval.MONTHLY;
      if (!monthly && interval != BillingInterval.YEARLY) {
        continue;
      }
      var start = usp.getSubscriptionStartDatetime();
      var end = usp.getSubscriptionEndDatetime();

      boolean startedBeforePeriod = start == null || start.isBefore(fromInstant);
      boolean notExpiredAtPeriodStart = end == null || end.isAfter(fromInstant);
      if (startedBeforePeriod && notExpiredAtPeriodStart) {
        if (monthly) {
          baselineMonthly++;
        } else {
          baselineAnnual++;
        }
      }

      if (start != null && !start.isBefore(fromInstant) && start.isBefore(toInstant)) {
        int idx = bucketIndex.applyAsInt(start);
        if (idx >= 0) {
          (monthly ? newMonthly : newAnnual)[idx] += 1;
        }
      }

      if (end != null && !end.isBefore(fromInstant) && end.isBefore(toInstant)) {
        int idx = bucketIndex.applyAsInt(end);
        if (idx >= 0) {
          (monthly ? expiredMonthly : expiredAnnual)[idx] += 1;
        }
      }
    }
    return new SubscriptionFlows(
        baselineMonthly, baselineAnnual, newMonthly, newAnnual, expiredMonthly, expiredAnnual);
  }

  List<Long> activeMonthlySubscriptions() {
    return cumulativeNet(baselineMonthly, newMonthly, expiredMonthly);
  }

  List<Long> activeAnnualSubscriptions() {
    return cumulativeNet(baselineAnnual, newAnnual, expiredAnnual);
  }

  List<Long> newMonthlySubscriptions() {
    return toList(newMonthly);
  }

  List<Long> newAnnualSubscriptions() {
    return toList(newAnnual);
  }

  private static List<Long> cumulativeNet(long baseline, long[] added, long[] removed) {
    var list = new ArrayList<Long>(added.length);
    long running = baseline;
    for (int i = 0; i < added.length; i++) {
      running += added[i] - removed[i];
      list.add(running);
    }
    return list;
  }

  private static List<Long> toList(long[] values) {
    var list = new ArrayList<Long>(values.length);
    for (long value : values) {
      list.add(value);
    }
    return list;
  }
}
