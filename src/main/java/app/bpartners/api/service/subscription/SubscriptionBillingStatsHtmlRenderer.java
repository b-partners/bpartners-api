package app.bpartners.api.service.subscription;

import app.bpartners.api.endpoint.rest.model.BillingAmount;
import app.bpartners.api.endpoint.rest.model.ExtraAnalysisCreditPlanStats;
import app.bpartners.api.endpoint.rest.model.ExtraAnalysisCreditStats;
import app.bpartners.api.endpoint.rest.model.SoldItemStats;
import app.bpartners.api.endpoint.rest.model.SubscriptionBillingMonthlyStats;
import app.bpartners.api.endpoint.rest.model.SubscriptionBillingStats;
import app.bpartners.api.service.utils.TemplateResolverEngine;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;

@Component
@RequiredArgsConstructor
public class SubscriptionBillingStatsHtmlRenderer {
  static final String TEMPLATE = "subscription_billing_stats";
  private static final DateTimeFormatter DAY_FORMAT =
      DateTimeFormatter.ofPattern("d MMM yyyy", Locale.FRANCE);
  private static final DateTimeFormatter MONTH_FORMAT =
      DateTimeFormatter.ofPattern("MMM yyyy", Locale.FRANCE);

  private final TemplateResolverEngine templateResolverEngine;

  public String render(SubscriptionBillingStats stats) {
    return templateResolverEngine.parseTemplateResolver(TEMPLATE, toContext(stats));
  }

  private Context toContext(SubscriptionBillingStats stats) {
    var context = new Context();
    context.setVariable("fromLabel", dayLabel(stats.getFrom()));
    context.setVariable("toLabel", dayLabel(stats.getTo()));
    context.setVariable("kpis", kpis(stats));
    context.setVariable("salesRows", salesRows(stats));
    context.setVariable("salesTotalHt", euros(amountHt(stats.getTotal())));
    context.setVariable("salesTotalTtc", euros(amountTtc(stats.getTotal())));
    fillCreditsByPlan(context, stats.getExtraAnalysisCredits());
    context.setVariable("monthRows", monthRows(stats.getMonthlyBreakdown()));
    return context;
  }

  private List<Map<String, Object>> kpis(SubscriptionBillingStats stats) {
    var total = stats.getTotal();
    var credits = stats.getExtraAnalysisCredits();
    List<Map<String, Object>> kpis = new ArrayList<>();
    kpis.add(kpi("Total TTC", euros(amountTtc(total)), euros(amountHt(total)) + " HT", true));
    kpis.add(kpi("Total HT", euros(amountHt(total)), "hors taxes", false));
    kpis.add(
        kpi(
            "Abonnements mensuels",
            count(soldCount(stats.getMonthlySubscriptions())),
            "instalments facturés",
            false));
    kpis.add(
        kpi(
            "Abonnements annuels",
            count(soldCount(stats.getAnnualSubscriptions())),
            "années facturées",
            false));
    kpis.add(
        kpi(
            "Crédits vendus",
            count(creditsSold(credits)),
            count(purchaseCount(credits)) + " achats",
            false));
    kpis.add(
        kpi("Crédits consommés", count(nz(stats.getConsumedCredits())), "analyses (net)", false));
    return kpis;
  }

  private List<Map<String, Object>> salesRows(SubscriptionBillingStats stats) {
    var credits = stats.getExtraAnalysisCredits();
    List<Map<String, Object>> rows = new ArrayList<>();
    rows.add(
        salesRow(
            "Abonnements mensuels",
            "1 vente par instalment",
            count(soldCount(stats.getMonthlySubscriptions())),
            stats.getMonthlySubscriptions()));
    rows.add(
        salesRow(
            "Abonnements annuels",
            "1 vente par année",
            count(soldCount(stats.getAnnualSubscriptions())),
            stats.getAnnualSubscriptions()));
    rows.add(
        creditsSalesRow(
            "Crédits d'analyse", "prépayés", count(creditsSold(credits)) + " cr.", credits));
    rows.add(
        salesRow(
            "Overages",
            "modèle historique",
            count(soldCount(stats.getLegacyOverages())),
            stats.getLegacyOverages()));
    return rows;
  }

  private void fillCreditsByPlan(Context context, ExtraAnalysisCreditStats credits) {
    var byPlan = credits == null ? null : credits.getByPlan();
    var rows = new ArrayList<Map<String, Object>>();
    if (byPlan != null) {
      for (ExtraAnalysisCreditPlanStats plan : byPlan) {
        var row = new LinkedHashMap<String, Object>();
        row.put(
            "name",
            plan.getSubscriptionPlanName() == null ? "Sans plan" : plan.getSubscriptionPlanName());
        row.put("purchases", count(nz(plan.getPurchaseCount())));
        row.put("credits", count(nz(plan.getCreditsSold())));
        row.put("ht", euros(nz(plan.getAmountInCentsWithoutVat())));
        row.put("ttc", euros(nz(plan.getAmountInCentsWithVat())));
        rows.add(row);
      }
    }
    context.setVariable("plansEmpty", rows.isEmpty());
    context.setVariable("planRows", rows);
    context.setVariable("planTotalPurchases", count(purchaseCount(credits)));
    context.setVariable("planTotalCredits", count(creditsSold(credits)));
    context.setVariable("planTotalHt", euros(amountHt(credits)));
    context.setVariable("planTotalTtc", euros(amountTtc(credits)));
  }

  private List<Map<String, Object>> monthRows(List<SubscriptionBillingMonthlyStats> breakdown) {
    var rows = new ArrayList<Map<String, Object>>();
    if (breakdown == null) {
      return rows;
    }
    for (SubscriptionBillingMonthlyStats month : breakdown) {
      var row = new LinkedHashMap<String, Object>();
      row.put("month", monthLabel(month.getYearMonth()));
      row.put("monthly", count(soldCount(month.getMonthlySubscriptions())));
      row.put("annual", count(soldCount(month.getAnnualSubscriptions())));
      row.put("credits", count(creditsSold(month.getExtraAnalysisCredits())));
      row.put("consumed", count(nz(month.getConsumedCredits())));
      row.put("overages", count(soldCount(month.getLegacyOverages())));
      row.put("ht", euros(amountHt(month.getTotal())));
      row.put("ttc", euros(amountTtc(month.getTotal())));
      rows.add(row);
    }
    return rows;
  }

  private Map<String, Object> kpi(String label, String value, String sub, boolean accent) {
    var kpi = new LinkedHashMap<String, Object>();
    kpi.put("label", label);
    kpi.put("value", value);
    kpi.put("sub", sub);
    kpi.put("accent", accent);
    return kpi;
  }

  private Map<String, Object> salesRow(String label, String sub, String qty, SoldItemStats item) {
    return row(label, sub, qty, amountHt(item), amountTtc(item));
  }

  private Map<String, Object> creditsSalesRow(
      String label, String sub, String qty, ExtraAnalysisCreditStats item) {
    return row(label, sub, qty, amountHt(item), amountTtc(item));
  }

  private Map<String, Object> row(String label, String sub, String qty, long ht, long ttc) {
    var row = new LinkedHashMap<String, Object>();
    row.put("label", label);
    row.put("sub", sub);
    row.put("qty", qty);
    row.put("ht", euros(ht));
    row.put("ttc", euros(ttc));
    return row;
  }

  private long soldCount(SoldItemStats item) {
    return item == null ? 0L : nz(item.getSoldCount());
  }

  private long amountHt(SoldItemStats item) {
    return item == null ? 0L : nz(item.getAmountInCentsWithoutVat());
  }

  private long amountTtc(SoldItemStats item) {
    return item == null ? 0L : nz(item.getAmountInCentsWithVat());
  }

  private long amountHt(ExtraAnalysisCreditStats item) {
    return item == null ? 0L : nz(item.getAmountInCentsWithoutVat());
  }

  private long amountTtc(ExtraAnalysisCreditStats item) {
    return item == null ? 0L : nz(item.getAmountInCentsWithVat());
  }

  private long amountHt(BillingAmount amount) {
    return amount == null ? 0L : nz(amount.getAmountInCentsWithoutVat());
  }

  private long amountTtc(BillingAmount amount) {
    return amount == null ? 0L : nz(amount.getAmountInCentsWithVat());
  }

  private long creditsSold(ExtraAnalysisCreditStats item) {
    return item == null ? 0L : nz(item.getCreditsSold());
  }

  private long purchaseCount(ExtraAnalysisCreditStats item) {
    return item == null ? 0L : nz(item.getPurchaseCount());
  }

  private String dayLabel(LocalDate date) {
    return date == null ? "—" : date.format(DAY_FORMAT);
  }

  private String monthLabel(String yearMonth) {
    if (yearMonth == null) {
      return "—";
    }
    try {
      return YearMonth.parse(yearMonth).format(MONTH_FORMAT);
    } catch (RuntimeException e) {
      return yearMonth;
    }
  }

  private String euros(long cents) {
    return String.format(Locale.FRANCE, "%,.2f €", BigDecimal.valueOf(cents).movePointLeft(2));
  }

  private String count(long value) {
    return String.format(Locale.FRANCE, "%,d", value);
  }

  private long nz(Long value) {
    return value == null ? 0L : value;
  }
}
