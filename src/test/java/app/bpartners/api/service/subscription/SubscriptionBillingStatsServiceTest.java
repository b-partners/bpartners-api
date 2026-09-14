package app.bpartners.api.service.subscription;

import static app.bpartners.api.model.credit.CreditPurchaseStatus.COMPLETED;
import static app.bpartners.api.model.credit.CreditTransactionMovementType.DEBIT;
import static app.bpartners.api.model.credit.CreditTransactionType.CONSUMPTION;
import static app.bpartners.api.model.subscription.BillingInterval.MONTHLY;
import static app.bpartners.api.model.subscription.BillingInterval.YEARLY;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.model.ExtraAnalysisCreditPlanStats;
import app.bpartners.api.endpoint.rest.model.SubscriptionBillingStats;
import app.bpartners.api.model.UserSubscriptionProduct;
import app.bpartners.api.model.credit.CreditPurchase;
import app.bpartners.api.model.credit.CreditTransaction;
import app.bpartners.api.model.credit.CreditTransactionType;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.subscription.BillingInterval;
import app.bpartners.api.model.subscription.OverageInvoiceLine;
import app.bpartners.api.model.subscription.SubscriptionPayment;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.payment.UserSubscriptionConf;
import app.bpartners.api.repository.jpa.CreditPurchaseRepository;
import app.bpartners.api.repository.jpa.CreditTransactionRepository;
import app.bpartners.api.repository.jpa.InvoiceJpaRepository;
import app.bpartners.api.repository.jpa.SubscriptionPaymentRepository;
import app.bpartners.api.repository.jpa.UserSubscriptionProductJpaRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class SubscriptionBillingStatsServiceTest {
  private static final Instant IN_AUGUST = Instant.parse("2024-08-15T10:00:00Z");
  private static final Instant IN_SEPTEMBER = Instant.parse("2024-09-15T10:00:00Z");
  private static final String PRO_USER = "u-pro";
  private static final String NO_PLAN_USER = "u-none";

  SubscriptionPaymentRepository subscriptionPaymentRepository = mock();
  CreditPurchaseRepository creditPurchaseRepository = mock();
  CreditTransactionRepository creditTransactionRepository = mock();
  InvoiceJpaRepository invoiceJpaRepository = mock();
  UserSubscriptionProductJpaRepository userSubscriptionProductJpaRepository = mock();

  SubscriptionBillingStatsService subject =
      new SubscriptionBillingStatsService(
          subscriptionPaymentRepository,
          creditPurchaseRepository,
          creditTransactionRepository,
          invoiceJpaRepository,
          userSubscriptionProductJpaRepository,
          new UserSubscriptionConf("sys"));

  @Test
  void aggregates_all_sources_over_the_period_and_breaks_down_by_month() {
    var payments =
        List.of(
            payment(MONTHLY, 100L, 120L, IN_AUGUST),
            payment(MONTHLY, 200L, 240L, IN_AUGUST),
            payment(YEARLY, 1000L, 1200L, IN_AUGUST),
            payment(MONTHLY, 200L, 240L, IN_SEPTEMBER));
    var proPlan = SubscriptionProduct.builder().id("pro").name("Pro").build();
    var purchases =
        List.of(
            purchase(PRO_USER, 20L, 80L, 96L, 400L, IN_AUGUST),
            purchase(PRO_USER, 10L, 40L, 48L, 400L, IN_AUGUST),
            purchase(PRO_USER, 20L, 80L, 96L, 400L, IN_SEPTEMBER),
            purchase(NO_PLAN_USER, 5L, 50L, 60L, 1000L, IN_SEPTEMBER));
    var proSubscription =
        UserSubscriptionProduct.builder()
            .id(randomUUID().toString())
            .userId(PRO_USER)
            .subscriptionProduct(proPlan)
            .subscriptionStartDatetime(IN_AUGUST.minusSeconds(3600))
            .subscriptionEndDatetime(null)
            .build();
    var overages = List.of(new OverageInvoiceLine(IN_AUGUST, 4, "200/1", "2000/1"));
    var consumptions =
        List.of(
            consumption(CONSUMPTION, 10L, IN_AUGUST), consumption(CONSUMPTION, 7L, IN_SEPTEMBER));

    when(subscriptionPaymentRepository.findInvoicedBetween(any(), any())).thenReturn(payments);
    when(creditPurchaseRepository.findByStatusBetween(eq(COMPLETED), any(), any()))
        .thenReturn(purchases);
    when(userSubscriptionProductJpaRepository.findByUserIdIn(any()))
        .thenReturn(List.of(proSubscription));
    when(invoiceJpaRepository.findOverageLines(eq("sys"), any(), any(), any()))
        .thenReturn(overages);
    when(creditTransactionRepository.findByTypesBetween(any(), any(), any()))
        .thenReturn(consumptions);

    SubscriptionBillingStats actual =
        subject.getStats(LocalDate.of(2024, 8, 1), LocalDate.of(2024, 9, 30));

    assertEquals(3L, actual.getMonthlySubscriptions().getSoldCount());
    assertEquals(500L, actual.getMonthlySubscriptions().getAmountInCentsWithoutVat());
    assertEquals(600L, actual.getMonthlySubscriptions().getAmountInCentsWithVat());
    assertEquals(1L, actual.getAnnualSubscriptions().getSoldCount());
    assertEquals(1000L, actual.getAnnualSubscriptions().getAmountInCentsWithoutVat());

    assertEquals(4L, actual.getExtraAnalysisCredits().getPurchaseCount());
    assertEquals(55L, actual.getExtraAnalysisCredits().getCreditsSold());
    assertEquals(250L, actual.getExtraAnalysisCredits().getAmountInCentsWithoutVat());
    assertEquals(300L, actual.getExtraAnalysisCredits().getAmountInCentsWithVat());

    List<ExtraAnalysisCreditPlanStats> byPlan = actual.getExtraAnalysisCredits().getByPlan();
    assertEquals(2, byPlan.size());
    assertEquals("pro", byPlan.get(0).getSubscriptionPlanId());
    assertEquals("Pro", byPlan.get(0).getSubscriptionPlanName());
    assertEquals(3L, byPlan.get(0).getPurchaseCount());
    assertEquals(50L, byPlan.get(0).getCreditsSold());
    assertEquals(240L, byPlan.get(0).getAmountInCentsWithVat());
    assertEquals(null, byPlan.get(1).getSubscriptionPlanId());
    assertEquals("Sans plan (10,00 € / crédit)", byPlan.get(1).getSubscriptionPlanName());
    assertEquals(5L, byPlan.get(1).getCreditsSold());

    assertEquals(4L, actual.getLegacyOverages().getSoldCount());
    assertEquals(800L, actual.getLegacyOverages().getAmountInCentsWithoutVat());
    assertEquals(960L, actual.getLegacyOverages().getAmountInCentsWithVat());

    assertEquals(17L, actual.getConsumedCredits());

    assertEquals(2550L, actual.getTotal().getAmountInCentsWithoutVat());
    assertEquals(3060L, actual.getTotal().getAmountInCentsWithVat());

    assertEquals(2, actual.getMonthlyBreakdown().size());
    var augustStats = actual.getMonthlyBreakdown().get(0);
    assertEquals("2024-08", augustStats.getYearMonth());
    assertEquals(10L, augustStats.getConsumedCredits());
    assertEquals(2220L, augustStats.getTotal().getAmountInCentsWithoutVat());
    var septemberStats = actual.getMonthlyBreakdown().get(1);
    assertEquals("2024-09", septemberStats.getYearMonth());
    assertEquals(7L, septemberStats.getConsumedCredits());
    assertEquals(330L, septemberStats.getTotal().getAmountInCentsWithoutVat());
  }

  @Test
  void rejects_period_whose_end_is_before_its_start() {
    assertThrows(
        BadRequestException.class,
        () -> subject.getStats(LocalDate.of(2024, 9, 2), LocalDate.of(2024, 9, 1)));
  }

  private static SubscriptionPayment payment(
      BillingInterval interval, long ht, long ttc, Instant paymentDatetime) {
    return SubscriptionPayment.builder()
        .id(randomUUID().toString())
        .userId(PRO_USER)
        .billingInterval(interval)
        .amountInCentsWithoutVat(ht)
        .amountInCentsWithVat(ttc)
        .paymentDatetime(paymentDatetime)
        .invoiceId("inv-" + randomUUID())
        .build();
  }

  private static CreditPurchase purchase(
      String userId, long credits, long ht, long ttc, long unitPrice, Instant completionDatetime) {
    return CreditPurchase.builder()
        .id(randomUUID().toString())
        .userId(userId)
        .credits(credits)
        .amountInCentsWithoutVat(ht)
        .amountInCentsWithVat(ttc)
        .creditUnitPriceInCentsWithoutVat(unitPrice)
        .status(COMPLETED)
        .completionDatetime(completionDatetime)
        .build();
  }

  private static CreditTransaction consumption(
      CreditTransactionType type, long credits, Instant creationDatetime) {
    return CreditTransaction.builder()
        .id(randomUUID().toString())
        .userId(PRO_USER)
        .type(type)
        .movementType(DEBIT)
        .credits(credits)
        .creationDatetime(creationDatetime)
        .build();
  }
}
