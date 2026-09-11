package app.bpartners.api.unit;

import static app.bpartners.api.model.subscription.SubscriptionConsumptionType.ROOF_ANALYSIS;
import static app.bpartners.api.model.subscription.SubscriptionConsumptionUnit.UNIT;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.rest.controller.SubscriptionController;
import app.bpartners.api.endpoint.rest.mapper.SubscriptionConsumptionLogRestMapper;
import app.bpartners.api.endpoint.rest.mapper.SubscriptionPlanRestMapper;
import app.bpartners.api.endpoint.rest.model.BillingAmount;
import app.bpartners.api.endpoint.rest.model.ConsumptionType;
import app.bpartners.api.endpoint.rest.model.ConsumptionUnit;
import app.bpartners.api.endpoint.rest.model.ExtraAnalysisCreditPlanStats;
import app.bpartners.api.endpoint.rest.model.ExtraAnalysisCreditStats;
import app.bpartners.api.endpoint.rest.model.SoldItemStats;
import app.bpartners.api.endpoint.rest.model.SubscriptionBillingMonthlyStats;
import app.bpartners.api.endpoint.rest.model.SubscriptionBillingStats;
import app.bpartners.api.endpoint.rest.model.SubscriptionBillingType;
import app.bpartners.api.endpoint.rest.model.SubscriptionPlan;
import app.bpartners.api.model.subscription.SubscriptionConsumptionLog;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.service.subscription.SubscriptionBillingStatsHtmlRenderer;
import app.bpartners.api.service.subscription.SubscriptionBillingStatsService;
import app.bpartners.api.service.subscription.SubscriptionService;
import app.bpartners.api.service.utils.TemplateResolverEngine;
import app.bpartners.api.service.utils.TemporalUtils;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class SubscriptionControllerTest {
  SubscriptionConsumptionLogRestMapper consumptionLogRestMapper =
      new SubscriptionConsumptionLogRestMapper();
  SubscriptionPlanRestMapper subscriptionPlanRestMapper = new SubscriptionPlanRestMapper();
  SubscriptionService subscriptionServiceMock = mock(SubscriptionService.class);
  EventProducer eventProducerMock = mock(EventProducer.class);
  TemporalUtils temporalUtils = new TemporalUtils();

  SubscriptionBillingStatsService subscriptionBillingStatsServiceMock =
      mock(SubscriptionBillingStatsService.class);
  SubscriptionBillingStatsHtmlRenderer subscriptionBillingStatsHtmlRenderer =
      new SubscriptionBillingStatsHtmlRenderer(new TemplateResolverEngine());

  SubscriptionController subject =
      new SubscriptionController(
          eventProducerMock,
          subscriptionServiceMock,
          consumptionLogRestMapper,
          subscriptionPlanRestMapper,
          subscriptionBillingStatsServiceMock,
          subscriptionBillingStatsHtmlRenderer);

  @Test
  void get_subscription_plans() {
    var page = new app.bpartners.api.model.PageFromOne(1);
    var pageSize = new app.bpartners.api.model.BoundedPageSize(100);
    when(subscriptionServiceMock.getSubscribablePlans(page, pageSize))
        .thenReturn(
            List.of(
                SubscriptionProduct.builder()
                    .id("essential")
                    .name("Essentiel")
                    .description("desc")
                    .features(List.of("f1"))
                    .billingType(
                        app.bpartners.api.model.subscription.SubscriptionBillingType.COMMITMENT)
                    .priceInCentsWithoutVat(4900L)
                    .vatPercent(2000L)
                    .includedCreditsPerBillingPeriod(10L)
                    .creditCostPerAnalysis(2L)
                    .freeUsageThreshold(20L)
                    .overageUnitPriceInCents(200L)
                    .trialPeriodDays(7)
                    .mostChosen(true)
                    .deprecated(true)
                    .displayPosition(3)
                    .build()));

    var actual = subject.getSubscriptionPlans(page, pageSize);

    assertEquals(
        List.of(
            new SubscriptionPlan()
                .id("essential")
                .name("Essentiel")
                .description("desc")
                .features(List.of("f1"))
                .inheritedFromPlanName(null)
                .billingType(SubscriptionBillingType.COMMITMENT)
                .priceInCentsWithVat(5880L)
                .priceInCentsWithoutVat(4900L)
                .vatPercent(2000L)
                .includedCreditsPerBillingPeriod(10L)
                .creditCostPerAnalysis(2L)
                .freeUsageThreshold(20L)
                .overageUnitPriceInCents(200L)
                .trialPeriodDays(7)
                .isMostChosen(true)
                .isDeprecated(true)
                .displayPosition(3)),
        actual);
  }

  @Test
  void get_consumptions_log_by_user_id() {
    var userId = randomUUID().toString();
    var startOfMonth = temporalUtils.startOfMonth();
    var endOfMonth = temporalUtils.endOfMonth();
    when(subscriptionServiceMock.findConsumptionLogsByUserId(userId, startOfMonth, endOfMonth))
        .thenReturn(
            List.of(
                SubscriptionConsumptionLog.builder()
                    .consumptionType(ROOF_ANALYSIS)
                    .consumptionUnit(UNIT)
                    .build()));
    var expected =
        List.of(
            new app.bpartners.api.endpoint.rest.model.SubscriptionConsumptionLog()
                .consumptionType(ConsumptionType.ROOF_ANALYSIS)
                .consumptionUnit(ConsumptionUnit.UNIT));

    var actual = subject.getConsumptionLogsByUserId(userId, startOfMonth, endOfMonth);

    assertEquals(expected, actual);
  }

  @Test
  void get_subscription_billing_stats_returns_html_by_default() {
    var from = LocalDate.of(2025, 1, 1);
    var to = LocalDate.of(2025, 3, 31);
    when(subscriptionBillingStatsServiceMock.getStats(from, to))
        .thenReturn(aSubscriptionBillingStats(from, to));

    var response = subject.getSubscriptionBillingStats(from, to, null);

    assertEquals(MediaType.TEXT_HTML, response.getHeaders().getContentType());
    var body = (String) response.getBody();
    assertNotNull(body);
    assertTrue(body.contains("BIRDIA"));
    assertTrue(body.contains("Statistiques de facturation des abonnements"));
    assertTrue(body.contains("Total TTC"));
    assertTrue(body.contains("Pro"));
  }

  @Test
  void get_subscription_billing_stats_returns_json_when_requested() {
    var from = LocalDate.of(2025, 1, 1);
    var to = LocalDate.of(2025, 3, 31);
    var stats = aSubscriptionBillingStats(from, to);
    when(subscriptionBillingStatsServiceMock.getStats(from, to)).thenReturn(stats);

    var response = subject.getSubscriptionBillingStats(from, to, MediaType.APPLICATION_JSON_VALUE);

    assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
    assertSame(stats, response.getBody());
  }

  private static SubscriptionBillingStats aSubscriptionBillingStats(LocalDate from, LocalDate to) {
    return new SubscriptionBillingStats()
        .from(from)
        .to(to)
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
