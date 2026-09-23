package app.bpartners.api.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.controller.SubscriptionBillingStatsController;
import app.bpartners.api.endpoint.rest.model.BillingAmount;
import app.bpartners.api.endpoint.rest.model.ExtraAnalysisCreditPlanStats;
import app.bpartners.api.endpoint.rest.model.ExtraAnalysisCreditStats;
import app.bpartners.api.endpoint.rest.model.SoldItemStats;
import app.bpartners.api.endpoint.rest.model.SubscriptionBillingStats;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.service.subscription.BillingGranularity;
import app.bpartners.api.service.subscription.SubscriptionBillingStatsHtmlRenderer;
import app.bpartners.api.service.subscription.SubscriptionBillingStatsService;
import app.bpartners.api.service.subscription.SubscriptionBillingTimeSeries;
import app.bpartners.api.service.subscription.SubscriptionBillingTimeSeriesService;
import app.bpartners.api.service.utils.TemplateResolverEngine;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class SubscriptionBillingStatsControllerTest {
  SubscriptionBillingStatsService statsServiceMock = mock(SubscriptionBillingStatsService.class);
  SubscriptionBillingTimeSeriesService timeSeriesServiceMock =
      mock(SubscriptionBillingTimeSeriesService.class);
  SubscriptionBillingStatsHtmlRenderer renderer =
      new SubscriptionBillingStatsHtmlRenderer(new TemplateResolverEngine());

  SubscriptionBillingStatsController subject =
      new SubscriptionBillingStatsController(statsServiceMock, timeSeriesServiceMock, renderer);

  @Test
  void returns_html_report_with_form_and_charts_by_default() {
    var from = LocalDate.of(2025, 1, 1);
    var to = LocalDate.of(2025, 3, 31);
    when(statsServiceMock.getStats(from, to)).thenReturn(aSubscriptionBillingStats(from, to));
    when(timeSeriesServiceMock.getTimeSeries(from, to, BillingGranularity.DAY))
        .thenReturn(aTimeSeries());

    var response = subject.getSubscriptionBillingStats(from, to, null, null);

    var contentType = response.getHeaders().getContentType();
    assertTrue(contentType.isCompatibleWith(MediaType.TEXT_HTML));
    assertEquals(StandardCharsets.UTF_8, contentType.getCharset());
    var body = (String) response.getBody();
    assertNotNull(body);
    assertTrue(body.contains("BIRDIA"));
    assertTrue(body.contains("Statistiques de facturation des abonnements"));
    assertTrue(body.contains("Total TTC"));
    assertTrue(body.contains("Périodicité"));
    assertTrue(body.contains("chartOverage"));
    assertTrue(body.contains("chartNewSubs"));
    assertTrue(body.contains("chartBilled"));
    assertTrue(body.contains("chart.js"));
    assertTrue(body.contains("Se déconnecter"));
    assertTrue(body.contains("2025-01-01"));
  }

  @Test
  void returns_json_when_requested() {
    var from = LocalDate.of(2025, 1, 1);
    var to = LocalDate.of(2025, 3, 31);
    var stats = aSubscriptionBillingStats(from, to);
    when(statsServiceMock.getStats(from, to)).thenReturn(stats);

    var response =
        subject.getSubscriptionBillingStats(from, to, null, MediaType.APPLICATION_JSON_VALUE);

    assertEquals(MediaType.APPLICATION_JSON, response.getHeaders().getContentType());
    assertSame(stats, response.getBody());
  }

  @Test
  void defaults_to_the_current_month_when_no_dates_given() {
    var today = LocalDate.now(java.time.ZoneId.of("Europe/Paris"));
    var expectedFrom = today.withDayOfMonth(1);
    when(statsServiceMock.getStats(expectedFrom, today))
        .thenReturn(aSubscriptionBillingStats(expectedFrom, today));
    when(timeSeriesServiceMock.getTimeSeries(expectedFrom, today, BillingGranularity.DAY))
        .thenReturn(aTimeSeries());

    var response = subject.getSubscriptionBillingStats(null, null, null, null);

    assertTrue(response.getHeaders().getContentType().isCompatibleWith(MediaType.TEXT_HTML));
    assertNotNull(response.getBody());
  }

  @Test
  void rejects_an_unknown_granularity() {
    var from = LocalDate.of(2025, 1, 1);
    var to = LocalDate.of(2025, 3, 31);
    when(statsServiceMock.getStats(any(), any())).thenReturn(aSubscriptionBillingStats(from, to));

    assertThrows(
        BadRequestException.class,
        () -> subject.getSubscriptionBillingStats(from, to, "YEAR", null));
  }

  @Test
  void signin_page_renders_an_api_key_form() {
    var response = subject.signinPage(null);

    var body = response.getBody();
    assertNotNull(body);
    assertTrue(body.contains("Accès au rapport de facturation"));
    assertTrue(body.contains("name=\"apiKey\""));
    assertTrue(body.contains("method=\"post\""));
  }

  @Test
  void submitting_the_key_sets_an_httponly_cookie_and_redirects_to_the_dashboard() {
    var request = new org.springframework.mock.web.MockHttpServletRequest();
    request.setSecure(true);

    var response = subject.submitSignin("the-admin-key", request);

    assertEquals(302, response.getStatusCode().value());
    assertEquals("/subscriptionBillingStats", response.getHeaders().getLocation().toString());
    var setCookie = response.getHeaders().getFirst(org.springframework.http.HttpHeaders.SET_COOKIE);
    assertNotNull(setCookie);
    assertTrue(setCookie.contains("x-api-key=the-admin-key"));
    assertTrue(setCookie.contains("HttpOnly"));
    assertTrue(setCookie.contains("Secure"));
    assertTrue(setCookie.contains("Path=/subscriptionBillingStats"));
  }

  @Test
  void submitting_a_blank_key_redirects_back_to_signin_with_error() {
    var request = new org.springframework.mock.web.MockHttpServletRequest();

    var response = subject.submitSignin("  ", request);

    assertEquals(302, response.getStatusCode().value());
    assertEquals(
        "/subscriptionBillingStats/signin?error", response.getHeaders().getLocation().toString());
  }

  @Test
  void logout_clears_the_cookie_and_redirects_to_signin() {
    var request = new org.springframework.mock.web.MockHttpServletRequest();

    var response = subject.logout(request);

    assertEquals(302, response.getStatusCode().value());
    assertEquals(
        "/subscriptionBillingStats/signin", response.getHeaders().getLocation().toString());
    var setCookie = response.getHeaders().getFirst(org.springframework.http.HttpHeaders.SET_COOKIE);
    assertNotNull(setCookie);
    assertTrue(setCookie.contains("x-api-key=;") || setCookie.contains("Max-Age=0"));
  }

  private static SubscriptionBillingTimeSeries aTimeSeries() {
    return SubscriptionBillingTimeSeries.builder()
        .granularity(BillingGranularity.DAY)
        .labels(List.of("1 janv.", "2 janv."))
        .activeMonthlySubscriptions(List.of(3L, 4L))
        .activeAnnualSubscriptions(List.of(1L, 1L))
        .newMonthlySubscriptions(List.of(1L, 0L))
        .newAnnualSubscriptions(List.of(0L, 1L))
        .billedMonthlyInstalments(List.of(2L, 1L))
        .billedAnnualInstalments(List.of(0L, 1L))
        .requestsWithoutPlan(List.of(0L, 2L))
        .overageRequestsByPlan(
            List.of(
                SubscriptionBillingTimeSeries.PlanSeries.builder()
                    .planName("Pro")
                    .values(List.of(0L, 1L))
                    .build()))
        .paidInvoicesCount(List.of(1L, 0L))
        .paidInvoicesAmountInCentsWithVat(List.of(12000L, 0L))
        .build();
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
        .monthlyBreakdown(List.of());
  }
}
