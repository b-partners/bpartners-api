package app.bpartners.api.service.subscription;

import static app.bpartners.api.model.credit.CreditPurchaseStatus.COMPLETED;
import static app.bpartners.api.model.credit.CreditTransactionType.CONSUMPTION;
import static app.bpartners.api.model.credit.CreditTransactionType.CONSUMPTION_REVERSAL;

import app.bpartners.api.endpoint.rest.model.BillingAmount;
import app.bpartners.api.endpoint.rest.model.ExtraAnalysisCreditPlanStats;
import app.bpartners.api.endpoint.rest.model.ExtraAnalysisCreditStats;
import app.bpartners.api.endpoint.rest.model.SoldItemStats;
import app.bpartners.api.endpoint.rest.model.SubscriptionBillingMonthlyStats;
import app.bpartners.api.endpoint.rest.model.SubscriptionBillingStats;
import app.bpartners.api.model.UserSubscriptionProduct;
import app.bpartners.api.model.credit.CreditPurchase;
import app.bpartners.api.model.credit.CreditTransaction;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.subscription.BillingInterval;
import app.bpartners.api.model.subscription.OverageInvoiceLine;
import app.bpartners.api.model.subscription.ResolvedSubscriptionPlan;
import app.bpartners.api.model.subscription.SubscriptionPayment;
import app.bpartners.api.payment.UserSubscriptionConf;
import app.bpartners.api.repository.jpa.CreditPurchaseRepository;
import app.bpartners.api.repository.jpa.CreditTransactionRepository;
import app.bpartners.api.repository.jpa.InvoiceJpaRepository;
import app.bpartners.api.repository.jpa.SubscriptionPaymentRepository;
import app.bpartners.api.repository.jpa.UserSubscriptionProductJpaRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriptionBillingStatsService {
  private static final ZoneId PARIS_ZONE = ZoneId.of("Europe/Paris");
  public static final String OVERAGE_PRODUCT_DESCRIPTION = "Analyse de toîtures supplémentaire";
  private static final long VAT_BASIS_POINTS = 10_000L;

  private final SubscriptionPaymentRepository subscriptionPaymentRepository;
  private final CreditPurchaseRepository creditPurchaseRepository;
  private final CreditTransactionRepository creditTransactionRepository;
  private final InvoiceJpaRepository invoiceJpaRepository;
  private final UserSubscriptionProductJpaRepository userSubscriptionProductJpaRepository;
  private final UserSubscriptionConf userSubscriptionConf;

  public SubscriptionBillingStats getStats(LocalDate from, LocalDate to) {
    if (from == null || to == null) {
      throw new BadRequestException("Both 'from' and 'to' query parameters are required");
    }
    if (to.isBefore(from)) {
      throw new BadRequestException("'from' must be before or equal to 'to'");
    }
    var fromInstant = from.atStartOfDay(PARIS_ZONE).toInstant();
    var toInstant = to.plusDays(1).atStartOfDay(PARIS_ZONE).toInstant();

    Map<YearMonth, MonthAccumulator> months = new TreeMap<>();

    accumulateSubscriptions(months, fromInstant, toInstant);
    accumulateCreditPurchases(months, fromInstant, toInstant);
    accumulateOverages(months, fromInstant, toInstant);
    accumulateConsumption(months, fromInstant, toInstant);

    var period = new MonthAccumulator();
    var monthlyBreakdown = new ArrayList<SubscriptionBillingMonthlyStats>();
    months.forEach(
        (yearMonth, acc) -> {
          period.merge(acc);
          monthlyBreakdown.add(
              new SubscriptionBillingMonthlyStats()
                  .yearMonth(yearMonth.toString())
                  .monthlySubscriptions(acc.monthly.toRest())
                  .annualSubscriptions(acc.annual.toRest())
                  .extraAnalysisCredits(acc.creditStats())
                  .consumedCredits(acc.consumedCredits)
                  .legacyOverages(acc.overage.toRest())
                  .total(acc.total()));
        });

    return new SubscriptionBillingStats()
        .from(from)
        .to(to)
        .monthlySubscriptions(period.monthly.toRest())
        .annualSubscriptions(period.annual.toRest())
        .extraAnalysisCredits(period.creditStats())
        .consumedCredits(period.consumedCredits)
        .legacyOverages(period.overage.toRest())
        .total(period.total())
        .monthlyBreakdown(monthlyBreakdown);
  }

  private void accumulateSubscriptions(
      Map<YearMonth, MonthAccumulator> months, Instant from, Instant to) {
    for (SubscriptionPayment payment :
        subscriptionPaymentRepository.findInvoicedBetween(from, to)) {
      var acc = monthAccumulator(months, payment.getPaymentDatetime());
      var target =
          BillingInterval.YEARLY == payment.getBillingInterval() ? acc.annual : acc.monthly;
      target.add(1L, payment.getAmountInCentsWithoutVat(), payment.getAmountInCentsWithVat());
    }
  }

  private void accumulateCreditPurchases(
      Map<YearMonth, MonthAccumulator> months, Instant from, Instant to) {
    var purchases = creditPurchaseRepository.findByStatusBetween(COMPLETED, from, to);
    var subscriptionsByUser = activeSubscriptionsByUser(purchases);
    for (CreditPurchase purchase : purchases) {
      var plan = resolveActivePlan(subscriptionsByUser, purchase);
      monthAccumulator(months, purchase.getCompletionDatetime()).addCreditSale(purchase, plan);
    }
  }

  private void accumulateOverages(
      Map<YearMonth, MonthAccumulator> months, Instant from, Instant to) {
    var lines =
        invoiceJpaRepository.findOverageLines(
            userSubscriptionConf.getUserToCreditId(), OVERAGE_PRODUCT_DESCRIPTION, from, to);
    for (OverageInvoiceLine line : lines) {
      long quantity = line.getQuantity() == null ? 0L : line.getQuantity();
      long unitPriceWithoutVat = fractionToLong(line.getUnitPrice());
      long vatBasisPoints = fractionToLong(line.getVatPercent());
      long amountWithoutVat = quantity * unitPriceWithoutVat;
      long amountWithVat =
          Math.round(
              amountWithoutVat * (VAT_BASIS_POINTS + vatBasisPoints) / (double) VAT_BASIS_POINTS);
      monthAccumulator(months, line.getCreatedDatetime())
          .overage
          .add(quantity, amountWithoutVat, amountWithVat);
    }
  }

  private void accumulateConsumption(
      Map<YearMonth, MonthAccumulator> months, Instant from, Instant to) {
    var transactions =
        creditTransactionRepository.findByTypesBetween(
            List.of(CONSUMPTION, CONSUMPTION_REVERSAL), from, to);
    for (CreditTransaction transaction : transactions) {
      long credits = transaction.getCredits() == null ? 0L : transaction.getCredits();
      long signed = transaction.getType() == CONSUMPTION_REVERSAL ? -credits : credits;
      monthAccumulator(months, transaction.getCreationDatetime()).consumedCredits += signed;
    }
  }

  private Map<String, List<UserSubscriptionProduct>> activeSubscriptionsByUser(
      List<CreditPurchase> purchases) {
    Set<String> userIds =
        purchases.stream().map(CreditPurchase::getUserId).collect(Collectors.toSet());
    if (userIds.isEmpty()) {
      return Map.of();
    }
    return userSubscriptionProductJpaRepository.findByUserIdIn(userIds).stream()
        .collect(Collectors.groupingBy(UserSubscriptionProduct::getUserId));
  }

  private ResolvedSubscriptionPlan resolveActivePlan(
      Map<String, List<UserSubscriptionProduct>> subscriptionsByUser, CreditPurchase purchase) {
    var completion = purchase.getCompletionDatetime();
    return subscriptionsByUser.getOrDefault(purchase.getUserId(), List.of()).stream()
        .filter(usp -> usp.getSubscriptionProduct() != null)
        .filter(usp -> isActiveAt(usp, completion))
        .max(
            Comparator.comparing(
                UserSubscriptionProduct::getSubscriptionStartDatetime,
                Comparator.nullsFirst(Comparator.naturalOrder())))
        .map(
            usp ->
                new ResolvedSubscriptionPlan(
                    usp.getSubscriptionProduct().getId(), usp.getSubscriptionProduct().getName()))
        .orElse(null);
  }

  private static boolean isActiveAt(UserSubscriptionProduct usp, Instant instant) {
    var start = usp.getSubscriptionStartDatetime();
    var end = usp.getSubscriptionEndDatetime();
    return (start == null || !start.isAfter(instant)) && (end == null || end.isAfter(instant));
  }

  private static long fractionToLong(String fraction) {
    if (fraction == null || fraction.isBlank()) {
      return 0L;
    }
    var parts = fraction.split("/");
    long numerator = Long.parseLong(parts[0].trim());
    long denominator = parts.length > 1 ? Long.parseLong(parts[1].trim()) : 1L;
    return denominator == 0L ? 0L : numerator / denominator;
  }

  private MonthAccumulator monthAccumulator(
      Map<YearMonth, MonthAccumulator> months, Instant instant) {
    var yearMonth = YearMonth.from(instant.atZone(PARIS_ZONE));
    return months.computeIfAbsent(yearMonth, k -> new MonthAccumulator());
  }

  private static final class SoldAccumulator {
    private long soldCount;
    private long amountWithoutVat;
    private long amountWithVat;

    private void add(long soldCount, Long amountWithoutVat, Long amountWithVat) {
      this.soldCount += soldCount;
      this.amountWithoutVat += orZero(amountWithoutVat);
      this.amountWithVat += orZero(amountWithVat);
    }

    private void merge(SoldAccumulator other) {
      this.soldCount += other.soldCount;
      this.amountWithoutVat += other.amountWithoutVat;
      this.amountWithVat += other.amountWithVat;
    }

    private SoldItemStats toRest() {
      return new SoldItemStats()
          .soldCount(soldCount)
          .amountInCentsWithoutVat(amountWithoutVat)
          .amountInCentsWithVat(amountWithVat);
    }
  }

  private static final class PlanAccumulator {
    private final String planId;
    private final String planName;
    private long purchaseCount;
    private long creditsSold;
    private long amountWithoutVat;
    private long amountWithVat;

    private PlanAccumulator(String planId, String planName) {
      this.planId = planId;
      this.planName = planName;
    }

    private void add(CreditPurchase purchase) {
      this.purchaseCount += 1;
      this.creditsSold += orZero(purchase.getCredits());
      this.amountWithoutVat += orZero(purchase.getAmountInCentsWithoutVat());
      this.amountWithVat += orZero(purchase.getAmountInCentsWithVat());
    }

    private void merge(PlanAccumulator other) {
      this.purchaseCount += other.purchaseCount;
      this.creditsSold += other.creditsSold;
      this.amountWithoutVat += other.amountWithoutVat;
      this.amountWithVat += other.amountWithVat;
    }

    private ExtraAnalysisCreditPlanStats toRest() {
      return new ExtraAnalysisCreditPlanStats()
          .subscriptionPlanId(planId)
          .subscriptionPlanName(planName)
          .purchaseCount(purchaseCount)
          .creditsSold(creditsSold)
          .amountInCentsWithoutVat(amountWithoutVat)
          .amountInCentsWithVat(amountWithVat);
    }
  }

  private static final class MonthAccumulator {
    private final SoldAccumulator monthly = new SoldAccumulator();
    private final SoldAccumulator annual = new SoldAccumulator();
    private final SoldAccumulator overage = new SoldAccumulator();
    private final Map<String, PlanAccumulator> creditsByPlan = new LinkedHashMap<>();
    private long consumedCredits;

    private void addCreditSale(CreditPurchase purchase, ResolvedSubscriptionPlan plan) {
      var key = planKeyOf(purchase, plan);
      creditsByPlan
          .computeIfAbsent(
              key, k -> new PlanAccumulator(planIdOf(plan), planNameOf(purchase, plan)))
          .add(purchase);
    }

    private void merge(MonthAccumulator other) {
      monthly.merge(other.monthly);
      annual.merge(other.annual);
      overage.merge(other.overage);
      consumedCredits += other.consumedCredits;
      other.creditsByPlan.forEach(
          (key, planAcc) ->
              creditsByPlan
                  .computeIfAbsent(key, k -> new PlanAccumulator(planAcc.planId, planAcc.planName))
                  .merge(planAcc));
    }

    private ExtraAnalysisCreditStats creditStats() {
      long purchaseCount = 0;
      long creditsSold = 0;
      long amountWithoutVat = 0;
      long amountWithVat = 0;
      var byPlan = new ArrayList<ExtraAnalysisCreditPlanStats>();
      var orderedPlans =
          creditsByPlan.values().stream()
              .sorted(Comparator.comparing(p -> p.planName == null ? "" : p.planName))
              .toList();
      for (var planAcc : orderedPlans) {
        purchaseCount += planAcc.purchaseCount;
        creditsSold += planAcc.creditsSold;
        amountWithoutVat += planAcc.amountWithoutVat;
        amountWithVat += planAcc.amountWithVat;
        byPlan.add(planAcc.toRest());
      }
      return new ExtraAnalysisCreditStats()
          .purchaseCount(purchaseCount)
          .creditsSold(creditsSold)
          .amountInCentsWithoutVat(amountWithoutVat)
          .amountInCentsWithVat(amountWithVat)
          .byPlan(byPlan);
    }

    private BillingAmount total() {
      long withoutVat =
          monthly.amountWithoutVat
              + annual.amountWithoutVat
              + overage.amountWithoutVat
              + creditsByPlan.values().stream().mapToLong(p -> p.amountWithoutVat).sum();
      long withVat =
          monthly.amountWithVat
              + annual.amountWithVat
              + overage.amountWithVat
              + creditsByPlan.values().stream().mapToLong(p -> p.amountWithVat).sum();
      return new BillingAmount().amountInCentsWithoutVat(withoutVat).amountInCentsWithVat(withVat);
    }
  }

  private static String planKeyOf(CreditPurchase purchase, ResolvedSubscriptionPlan plan) {
    if (plan != null && plan.getId() != null) {
      return "plan:" + plan.getId();
    }
    return "unit:" + purchase.getCreditUnitPriceInCentsWithoutVat();
  }

  private static String planIdOf(ResolvedSubscriptionPlan plan) {
    return plan == null ? null : plan.getId();
  }

  private static String planNameOf(CreditPurchase purchase, ResolvedSubscriptionPlan plan) {
    if (plan != null && plan.getName() != null) {
      return plan.getName();
    }
    var unitPrice = purchase.getCreditUnitPriceInCentsWithoutVat();
    if (unitPrice != null) {
      return "Sans plan ("
          + String.format(Locale.FRANCE, "%.2f €", unitPrice / 100.0)
          + " / crédit)";
    }
    return "Sans plan";
  }

  private static long orZero(Long value) {
    return value == null ? 0L : value;
  }
}
