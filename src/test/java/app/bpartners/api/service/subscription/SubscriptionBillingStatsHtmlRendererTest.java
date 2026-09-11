package app.bpartners.api.service.subscription;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.api.endpoint.rest.model.BillingAmount;
import app.bpartners.api.endpoint.rest.model.ExtraAnalysisCreditPlanStats;
import app.bpartners.api.endpoint.rest.model.ExtraAnalysisCreditStats;
import app.bpartners.api.endpoint.rest.model.SoldItemStats;
import app.bpartners.api.endpoint.rest.model.SubscriptionBillingMonthlyStats;
import app.bpartners.api.endpoint.rest.model.SubscriptionBillingStats;
import app.bpartners.api.service.utils.TemplateResolverEngine;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class SubscriptionBillingStatsHtmlRendererTest {
  SubscriptionBillingStatsHtmlRenderer subject =
      new SubscriptionBillingStatsHtmlRenderer(new TemplateResolverEngine());

  @Test
  void renders_full_report_with_french_labels_and_amounts() {
    var html = subject.render(aSubscriptionBillingStats());

    assertNotNull(html);
    assertTrue(html.contains("BIRDIA"));
    assertTrue(html.contains("Statistiques de facturation des abonnements"));
    assertTrue(html.contains("1 janv. 2025"));
    assertTrue(html.contains("31 mars 2025"));
    assertTrue(html.contains("janv. 2025"));
    assertTrue(html.contains("Starter"));
    assertTrue(html.contains("Pro"));
    assertTrue(html.contains("72,00 €"));
    assertTrue(html.contains("560,00 €"));
    assertFalse(html.contains("Aucune ventilation par plan"));
    assertFalse(html.contains("Aucun mois dans la période"));
  }

  @Test
  void renders_fallback_row_when_no_plan_breakdown() {
    var stats =
        aSubscriptionBillingStats()
            .extraAnalysisCredits(
                new ExtraAnalysisCreditStats()
                    .purchaseCount(3L)
                    .creditsSold(150L)
                    .amountInCentsWithoutVat(15000L)
                    .amountInCentsWithVat(18000L)
                    .byPlan(List.of()));

    var html = subject.render(stats);

    assertTrue(html.contains("Aucune ventilation par plan"));
    assertFalse(html.contains("Total crédits"));
  }

  @Test
  void renders_without_error_on_null_fields() {
    var html = subject.render(new SubscriptionBillingStats());

    assertNotNull(html);
    assertTrue(html.contains("Aucun mois dans la période"));
    assertTrue(html.contains("0,00 €"));
    assertTrue(html.contains("—"));
  }

  private static SubscriptionBillingStats aSubscriptionBillingStats() {
    return new SubscriptionBillingStats()
        .from(LocalDate.of(2025, 1, 1))
        .to(LocalDate.of(2025, 3, 31))
        .monthlySubscriptions(
            new SoldItemStats()
                .soldCount(45L)
                .amountInCentsWithoutVat(220500L)
                .amountInCentsWithVat(264600L))
        .annualSubscriptions(
            new SoldItemStats()
                .soldCount(6L)
                .amountInCentsWithoutVat(294000L)
                .amountInCentsWithVat(352800L))
        .extraAnalysisCredits(
            new ExtraAnalysisCreditStats()
                .purchaseCount(21L)
                .creditsSold(1170L)
                .amountInCentsWithoutVat(117000L)
                .amountInCentsWithVat(140400L)
                .byPlan(
                    List.of(
                        new ExtraAnalysisCreditPlanStats()
                            .subscriptionPlanId("plan_starter")
                            .subscriptionPlanName("Starter")
                            .purchaseCount(12L)
                            .creditsSold(610L)
                            .amountInCentsWithoutVat(61000L)
                            .amountInCentsWithVat(73200L),
                        new ExtraAnalysisCreditPlanStats()
                            .subscriptionPlanId("plan_pro")
                            .subscriptionPlanName("Pro")
                            .purchaseCount(9L)
                            .creditsSold(560L)
                            .amountInCentsWithoutVat(56000L)
                            .amountInCentsWithVat(67200L))))
        .consumedCredits(700L)
        .legacyOverages(
            new SoldItemStats()
                .soldCount(6L)
                .amountInCentsWithoutVat(6000L)
                .amountInCentsWithVat(7200L))
        .total(new BillingAmount().amountInCentsWithoutVat(637500L).amountInCentsWithVat(765000L))
        .monthlyBreakdown(
            List.of(
                new SubscriptionBillingMonthlyStats()
                    .yearMonth("2025-01")
                    .monthlySubscriptions(
                        new SoldItemStats()
                            .soldCount(12L)
                            .amountInCentsWithoutVat(58800L)
                            .amountInCentsWithVat(70560L))
                    .annualSubscriptions(
                        new SoldItemStats()
                            .soldCount(2L)
                            .amountInCentsWithoutVat(98000L)
                            .amountInCentsWithVat(117600L))
                    .extraAnalysisCredits(
                        new ExtraAnalysisCreditStats()
                            .purchaseCount(5L)
                            .creditsSold(250L)
                            .amountInCentsWithoutVat(25000L)
                            .amountInCentsWithVat(30000L))
                    .consumedCredits(180L)
                    .legacyOverages(
                        new SoldItemStats()
                            .soldCount(4L)
                            .amountInCentsWithoutVat(4000L)
                            .amountInCentsWithVat(4800L))
                    .total(
                        new BillingAmount()
                            .amountInCentsWithoutVat(185800L)
                            .amountInCentsWithVat(222960L))));
  }
}
