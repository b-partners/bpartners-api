package app.bpartners.api.service.subscription;

import static app.bpartners.api.model.credit.CreditTransactionType.CONSUMPTION;
import static app.bpartners.api.model.credit.CreditTransactionType.CONSUMPTION_REVERSAL;
import static app.bpartners.api.model.credit.CreditTransactionType.SUBSCRIPTION_GRANT;
import static app.bpartners.api.service.utils.FractionUtils.parseFraction;
import static java.util.Comparator.comparing;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;
import static java.util.stream.Collectors.groupingBy;

import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.model.UserSubscriptionProduct;
import app.bpartners.api.model.credit.CreditTransaction;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.mapper.InvoiceMapper;
import app.bpartners.api.model.mapper.InvoiceProductMapper;
import app.bpartners.api.model.subscription.BillingInterval;
import app.bpartners.api.repository.jpa.CreditTransactionRepository;
import app.bpartners.api.repository.jpa.InvoiceJpaRepository;
import app.bpartners.api.repository.jpa.UserSubscriptionProductJpaRepository;
import app.bpartners.api.repository.jpa.model.HInvoice;
import app.bpartners.api.repository.jpa.model.HInvoiceProduct;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriptionBillingTimeSeriesService {
  private static final ZoneId PARIS_ZONE = ZoneId.of("Europe/Paris");
  private static final String UNKNOWN_PLAN = "Plan inconnu";
  private static final DateTimeFormatter DAY_LABEL =
      DateTimeFormatter.ofPattern("d MMM", Locale.FRANCE);
  private static final DateTimeFormatter WEEK_LABEL =
      DateTimeFormatter.ofPattern("'sem.' d MMM", Locale.FRANCE);
  private static final DateTimeFormatter MONTH_LABEL =
      DateTimeFormatter.ofPattern("MMM yyyy", Locale.FRANCE);

  private final CreditTransactionRepository creditTransactionRepository;
  private final UserSubscriptionProductJpaRepository userSubscriptionProductJpaRepository;
  private final InvoiceJpaRepository invoiceJpaRepository;
  private final InvoiceProductMapper invoiceProductMapper;

  public SubscriptionBillingTimeSeries getTimeSeries(
      LocalDate from, LocalDate to, BillingGranularity granularity) {
    if (from == null || to == null) {
      throw new BadRequestException("Both 'from' and 'to' are required");
    }
    if (to.isBefore(from)) {
      throw new BadRequestException("'from' must be before or equal to 'to'");
    }
    var fromInstant = from.atStartOfDay(PARIS_ZONE).toInstant();
    var toInstant = to.plusDays(1).atStartOfDay(PARIS_ZONE).toInstant();

    List<LocalDate> buckets = buckets(from, to, granularity);
    int n = buckets.size();
    Map<LocalDate, Integer> indexByBucket = new HashMap<>();
    List<String> labels = new ArrayList<>(n);
    for (int i = 0; i < n; i++) {
      indexByBucket.put(buckets.get(i), i);
      labels.add(label(buckets.get(i), granularity));
    }

    var overlappingSubs =
        userSubscriptionProductJpaRepository.findAllOverlapping(fromInstant, toInstant);
    Map<String, List<UserSubscriptionProduct>> subsByUser =
        overlappingSubs.stream().collect(groupingBy(UserSubscriptionProduct::getUserId));

    List<Long> activeMonthly = new ArrayList<>(n);
    List<Long> activeAnnual = new ArrayList<>(n);
    for (LocalDate bucket : buckets) {
      var at = snapshotInstant(bucket, granularity, toInstant);
      activeMonthly.add(activeCount(overlappingSubs, at, BillingInterval.MONTHLY));
      activeAnnual.add(activeCount(overlappingSubs, at, BillingInterval.YEARLY));
    }

    LocalDate firstMonth = from.withDayOfMonth(1);
    LocalDate lastMonth = to.withDayOfMonth(1);
    var grants =
        creditTransactionRepository.findByTypeAndGrantPeriodStartBetween(
            SUBSCRIPTION_GRANT, firstMonth, lastMonth);
    Map<String, Map<LocalDate, Long>> grantedByUserMonth = new HashMap<>();
    for (CreditTransaction grant : grants) {
      if (grant.getSubscriptionProductId() == null || grant.getGrantPeriodStart() == null) {
        continue;
      }
      grantedByUserMonth
          .computeIfAbsent(grant.getUserId(), k -> new HashMap<>())
          .merge(grant.getGrantPeriodStart(), grant.creditsOrZero(), Long::sum);
    }

    var consumptionFetchFrom = firstMonth.atStartOfDay(PARIS_ZONE).toInstant();
    var consumptions =
        creditTransactionRepository.findByTypesBetween(
            List.of(CONSUMPTION, CONSUMPTION_REVERSAL), consumptionFetchFrom, toInstant);
    Map<String, List<CreditTransaction>> consumptionByUser =
        consumptions.stream().collect(groupingBy(CreditTransaction::getUserId));

    long[] withoutPlan = new long[n];
    Map<String, long[]> overageByPlan = new LinkedHashMap<>();
    for (var entry : consumptionByUser.entrySet()) {
      var userSubs = subsByUser.getOrDefault(entry.getKey(), List.of());
      var userGrants = grantedByUserMonth.getOrDefault(entry.getKey(), Map.of());
      Map<LocalDate, Long> cumByMonth = new HashMap<>();
      var ordered =
          entry.getValue().stream()
              .sorted(comparing(CreditTransaction::getCreationDatetime, nullsFirst(naturalOrder())))
              .toList();
      for (CreditTransaction txn : ordered) {
        var when = txn.getCreationDatetime();
        if (when == null) {
          continue;
        }
        boolean reversal = txn.getType() == CONSUMPTION_REVERSAL;
        long credits = txn.creditsOrZero();
        LocalDate month = when.atZone(PARIS_ZONE).toLocalDate().withDayOfMonth(1);
        long cumAfter = cumByMonth.getOrDefault(month, 0L) + (reversal ? -credits : credits);
        cumByMonth.put(month, cumAfter);
        if (when.isBefore(fromInstant)) {
          continue;
        }
        int idx = bucketIndexOf(indexByBucket, when, granularity);
        if (idx < 0) {
          continue;
        }
        var plan = resolvePlan(userSubs, when);
        if (plan == null) {
          withoutPlan[idx] += reversal ? -1 : 1;
        } else if (!reversal && cumAfter > userGrants.getOrDefault(month, 0L)) {
          overageByPlan.computeIfAbsent(planName(plan), k -> new long[n])[idx] += 1;
        }
      }
    }

    long[] paidCount = new long[n];
    long[] paidAmount = new long[n];
    var paidInvoices =
        invoiceJpaRepository.findByStatusAndCreatedDatetimeBetween(
            InvoiceStatus.PAID, fromInstant, toInstant);
    for (HInvoice invoice : paidInvoices) {
      var when = invoice.getCreatedDatetime();
      if (when == null) {
        continue;
      }
      int idx = bucketIndexOf(indexByBucket, when, granularity);
      if (idx < 0) {
        continue;
      }
      paidCount[idx] += 1;
      paidAmount[idx] += invoiceAmountInCentsWithVat(invoice);
    }

    return SubscriptionBillingTimeSeries.builder()
        .granularity(granularity)
        .labels(labels)
        .activeMonthlySubscriptions(activeMonthly)
        .activeAnnualSubscriptions(activeAnnual)
        .requestsWithoutPlan(toList(withoutPlan))
        .overageRequestsByPlan(planSeries(overageByPlan))
        .paidInvoicesCount(toList(paidCount))
        .paidInvoicesAmountInCentsWithVat(toList(paidAmount))
        .build();
  }

  private long invoiceAmountInCentsWithVat(HInvoice invoice) {
    var products =
        invoice.getProducts() == null ? List.<HInvoiceProduct>of() : invoice.getProducts();
    var domainProducts = products.stream().map(invoiceProductMapper::toDomain).toList();
    var discount = parseFraction(invoice.getDiscountPercent());
    return InvoiceMapper.computeTotalPriceWithVatAndDiscount(discount, domainProducts)
        .getCentsRoundUp();
  }

  private long activeCount(
      List<UserSubscriptionProduct> subs, Instant at, BillingInterval interval) {
    Set<String> userIds = new HashSet<>();
    for (UserSubscriptionProduct usp : subs) {
      if (usp.getBillingInterval() == interval && isActiveAt(usp, at)) {
        userIds.add(usp.getUserId());
      }
    }
    return userIds.size();
  }

  private UserSubscriptionProduct resolvePlan(
      List<UserSubscriptionProduct> userSubs, Instant when) {
    return userSubs.stream()
        .filter(usp -> usp.getSubscriptionProduct() != null)
        .filter(usp -> isActiveAt(usp, when))
        .max(
            comparing(
                UserSubscriptionProduct::getSubscriptionStartDatetime, nullsFirst(naturalOrder())))
        .orElse(null);
  }

  private static String planName(UserSubscriptionProduct usp) {
    var name = usp.getSubscriptionProduct().getName();
    return name == null ? UNKNOWN_PLAN : name;
  }

  private static boolean isActiveAt(UserSubscriptionProduct usp, Instant instant) {
    var start = usp.getSubscriptionStartDatetime();
    var end = usp.getSubscriptionEndDatetime();
    return (start == null || !start.isAfter(instant)) && (end == null || end.isAfter(instant));
  }

  private List<SubscriptionBillingTimeSeries.PlanSeries> planSeries(Map<String, long[]> byPlan) {
    var ordered = new TreeMap<>(byPlan);
    var result = new ArrayList<SubscriptionBillingTimeSeries.PlanSeries>();
    ordered.forEach(
        (plan, values) ->
            result.add(
                SubscriptionBillingTimeSeries.PlanSeries.builder()
                    .planName(plan)
                    .values(toList(values))
                    .build()));
    return result;
  }

  private List<LocalDate> buckets(LocalDate from, LocalDate to, BillingGranularity granularity) {
    var buckets = new ArrayList<LocalDate>();
    var cursor = bucketStartOf(from, granularity);
    var last = bucketStartOf(to, granularity);
    while (!cursor.isAfter(last)) {
      buckets.add(cursor);
      cursor = nextBucketStart(cursor, granularity);
    }
    return buckets;
  }

  private static LocalDate bucketStartOf(LocalDate date, BillingGranularity granularity) {
    return switch (granularity) {
      case DAY -> date;
      case WEEK -> date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
      case MONTH -> date.withDayOfMonth(1);
    };
  }

  private static LocalDate nextBucketStart(LocalDate bucketStart, BillingGranularity granularity) {
    return switch (granularity) {
      case DAY -> bucketStart.plusDays(1);
      case WEEK -> bucketStart.plusWeeks(1);
      case MONTH -> bucketStart.plusMonths(1);
    };
  }

  private Instant snapshotInstant(
      LocalDate bucketStart, BillingGranularity granularity, Instant toInstant) {
    var endExclusive =
        nextBucketStart(bucketStart, granularity).atStartOfDay(PARIS_ZONE).toInstant();
    return endExclusive.isAfter(toInstant) ? toInstant : endExclusive;
  }

  private int bucketIndexOf(
      Map<LocalDate, Integer> indexByBucket, Instant when, BillingGranularity granularity) {
    var key = bucketStartOf(when.atZone(PARIS_ZONE).toLocalDate(), granularity);
    return indexByBucket.getOrDefault(key, -1);
  }

  private static String label(LocalDate bucketStart, BillingGranularity granularity) {
    return switch (granularity) {
      case DAY -> bucketStart.format(DAY_LABEL);
      case WEEK -> bucketStart.format(WEEK_LABEL);
      case MONTH -> bucketStart.format(MONTH_LABEL);
    };
  }

  private static List<Long> toList(long[] values) {
    var list = new ArrayList<Long>(values.length);
    for (long value : values) {
      list.add(value);
    }
    return list;
  }
}
