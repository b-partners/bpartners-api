package app.bpartners.api.service.subscription;

import static app.bpartners.api.model.credit.CreditTransactionType.CONSUMPTION;
import static app.bpartners.api.model.credit.CreditTransactionType.CONSUMPTION_REVERSAL;
import static app.bpartners.api.model.credit.CreditTransactionType.SUBSCRIPTION_GRANT;
import static app.bpartners.api.model.subscription.BillingInterval.MONTHLY;
import static app.bpartners.api.model.subscription.BillingInterval.YEARLY;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.model.UserSubscriptionProduct;
import app.bpartners.api.model.credit.CreditTransaction;
import app.bpartners.api.model.credit.CreditTransactionType;
import app.bpartners.api.model.mapper.InvoiceProductMapper;
import app.bpartners.api.model.subscription.BillingInterval;
import app.bpartners.api.model.subscription.SubscriptionPayment;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.repository.jpa.CreditTransactionRepository;
import app.bpartners.api.repository.jpa.InvoiceJpaRepository;
import app.bpartners.api.repository.jpa.SubscriptionPaymentRepository;
import app.bpartners.api.repository.jpa.UserSubscriptionProductJpaRepository;
import app.bpartners.api.repository.jpa.model.HInvoice;
import app.bpartners.api.repository.jpa.model.HInvoiceProduct;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SubscriptionBillingTimeSeriesServiceTest {
  CreditTransactionRepository creditTransactionRepository = mock();
  UserSubscriptionProductJpaRepository userSubscriptionProductJpaRepository = mock();
  InvoiceJpaRepository invoiceJpaRepository = mock();
  SubscriptionPaymentRepository subscriptionPaymentRepository = mock();

  SubscriptionBillingTimeSeriesService subject =
      new SubscriptionBillingTimeSeriesService(
          creditTransactionRepository,
          userSubscriptionProductJpaRepository,
          invoiceJpaRepository,
          new InvoiceProductMapper(),
          subscriptionPaymentRepository);

  static final LocalDate FROM = LocalDate.of(2025, 1, 1);
  static final LocalDate TO = LocalDate.of(2025, 1, 31);

  @BeforeEach
  void setup() {
    when(userSubscriptionProductJpaRepository.findAllOverlapping(any(), any()))
        .thenReturn(List.of());
    when(creditTransactionRepository.findByTypeAndGrantPeriodStartBetween(any(), any(), any()))
        .thenReturn(List.of());
    when(creditTransactionRepository.findByTypesBetween(any(), any(), any())).thenReturn(List.of());
    when(invoiceJpaRepository.findByStatusAndCreatedDatetimeBetween(any(), any(), any()))
        .thenReturn(List.of());
    when(subscriptionPaymentRepository.findInvoicedBetween(any(), any())).thenReturn(List.of());
  }

  @Test
  void counts_pre_existing_subscriptions_as_active_baseline() {
    when(userSubscriptionProductJpaRepository.findAllOverlapping(any(), any()))
        .thenReturn(
            List.of(
                sub("u1", plan("p_m", "Mensuel"), MONTHLY, instant("2024-12-01"), null),
                sub("u2", plan("p_y", "Annuel"), YEARLY, instant("2024-06-01"), null)));

    var series = subject.getTimeSeries(FROM, TO, BillingGranularity.MONTH);

    assertEquals(List.of("janv. 2025"), series.getLabels());
    assertEquals(List.of(1L), series.getActiveMonthlySubscriptions());
    assertEquals(List.of(1L), series.getActiveAnnualSubscriptions());
    assertEquals(List.of(0L), series.getRequestsWithoutPlan());
    assertTrue(series.getOverageRequestsByPlan().isEmpty());
    assertEquals(List.of(0L), series.getPaidInvoicesCount());
  }

  @Test
  void active_subscriptions_accumulate_new_and_net_out_expirations_over_the_period() {
    when(userSubscriptionProductJpaRepository.findAllOverlapping(any(), any()))
        .thenReturn(
            List.of(
                sub("u_base", plan("p_m", "Mensuel"), MONTHLY, instant("2024-12-15"), null),
                sub(
                    "u_churn",
                    plan("p_m", "Mensuel"),
                    MONTHLY,
                    instant("2024-12-20"),
                    instant("2025-01-03")),
                sub("u_new", plan("p_m", "Mensuel"), MONTHLY, instant("2025-01-02"), null)));

    var series =
        subject.getTimeSeries(
            LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 3), BillingGranularity.DAY);

    assertEquals(List.of(2L, 3L, 2L), series.getActiveMonthlySubscriptions());
    assertEquals(List.of(0L, 0L, 0L), series.getActiveAnnualSubscriptions());
  }

  @Test
  void counts_new_subscriptions_by_start_and_billed_instalments_by_payment() {
    when(userSubscriptionProductJpaRepository.findAllOverlapping(any(), any()))
        .thenReturn(
            List.of(
                sub("u_new_m", plan("p_m", "Mensuel"), MONTHLY, instant("2025-01-10"), null),
                sub("u_new_y", plan("p_y", "Annuel"), YEARLY, instant("2025-01-20"), null),
                sub("u_old", plan("p_m", "Mensuel"), MONTHLY, instant("2024-12-01"), null)));
    when(subscriptionPaymentRepository.findInvoicedBetween(any(), any()))
        .thenReturn(
            List.of(
                payment(MONTHLY, instant("2025-01-05")),
                payment(MONTHLY, instant("2025-01-15")),
                payment(YEARLY, instant("2025-01-25"))));

    var series = subject.getTimeSeries(FROM, TO, BillingGranularity.MONTH);

    assertEquals(List.of(1L), series.getNewMonthlySubscriptions());
    assertEquals(List.of(1L), series.getNewAnnualSubscriptions());
    assertEquals(List.of(2L), series.getBilledMonthlyInstalments());
    assertEquals(List.of(1L), series.getBilledAnnualInstalments());
  }

  @Test
  void counts_requests_without_plan_net_of_reversals() {
    when(creditTransactionRepository.findByTypesBetween(any(), any(), any()))
        .thenReturn(
            List.of(
                consumption("u3", CONSUMPTION, 1L, instant("2025-01-05")),
                consumption("u3", CONSUMPTION, 1L, instant("2025-01-06")),
                consumption("u3", CONSUMPTION_REVERSAL, 1L, instant("2025-01-07"))));

    var series = subject.getTimeSeries(FROM, TO, BillingGranularity.MONTH);

    assertEquals(List.of(1L), series.getRequestsWithoutPlan());
    assertTrue(series.getOverageRequestsByPlan().isEmpty());
  }

  @Test
  void counts_only_requests_beyond_the_monthly_quota_grouped_by_plan() {
    when(userSubscriptionProductJpaRepository.findAllOverlapping(any(), any()))
        .thenReturn(List.of(sub("u4", plan("p_pro", "Pro"), MONTHLY, instant("2024-12-01"), null)));
    when(creditTransactionRepository.findByTypeAndGrantPeriodStartBetween(
            SUBSCRIPTION_GRANT, LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 1)))
        .thenReturn(List.of(grant("u4", "p_pro", LocalDate.of(2025, 1, 1), 5L)));
    when(creditTransactionRepository.findByTypesBetween(any(), any(), any()))
        .thenReturn(
            List.of(
                consumption("u4", CONSUMPTION, 1L, instant("2025-01-02")),
                consumption("u4", CONSUMPTION, 1L, instant("2025-01-03")),
                consumption("u4", CONSUMPTION, 1L, instant("2025-01-04")),
                consumption("u4", CONSUMPTION, 1L, instant("2025-01-05")),
                consumption("u4", CONSUMPTION, 1L, instant("2025-01-06")),
                consumption("u4", CONSUMPTION, 1L, instant("2025-01-07")),
                consumption("u4", CONSUMPTION, 1L, instant("2025-01-08"))));

    var series = subject.getTimeSeries(FROM, TO, BillingGranularity.MONTH);

    assertEquals(List.of(0L), series.getRequestsWithoutPlan());
    assertEquals(1, series.getOverageRequestsByPlan().size());
    var proSeries = series.getOverageRequestsByPlan().get(0);
    assertEquals("Pro", proSeries.getPlanName());
    assertEquals(List.of(2L), proSeries.getValues());
  }

  @Test
  void sums_paid_invoices_count_and_amount() {
    when(invoiceJpaRepository.findByStatusAndCreatedDatetimeBetween(any(), any(), any()))
        .thenReturn(List.of(paidInvoice(instant("2025-01-10"), 2, "5000/1", "2000/1")));

    var series = subject.getTimeSeries(FROM, TO, BillingGranularity.MONTH);

    assertEquals(List.of(1L), series.getPaidInvoicesCount());
    assertEquals(List.of(12000L), series.getPaidInvoicesAmountInCentsWithVat());
  }

  @Test
  void spreads_daily_buckets_over_the_whole_period() {
    var series =
        subject.getTimeSeries(
            LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 3), BillingGranularity.DAY);

    assertEquals(3, series.getLabels().size());
    assertEquals(3, series.getActiveMonthlySubscriptions().size());
  }

  private static Instant instant(String isoDate) {
    return LocalDate.parse(isoDate).atStartOfDay(java.time.ZoneId.of("Europe/Paris")).toInstant();
  }

  private static SubscriptionProduct plan(String id, String name) {
    return SubscriptionProduct.builder().id(id).name(name).build();
  }

  private static UserSubscriptionProduct sub(
      String userId,
      SubscriptionProduct plan,
      BillingInterval interval,
      Instant start,
      Instant end) {
    return UserSubscriptionProduct.builder()
        .id(randomUUID().toString())
        .userId(userId)
        .subscriptionProduct(plan)
        .billingInterval(interval)
        .subscriptionStartDatetime(start)
        .subscriptionEndDatetime(end)
        .build();
  }

  private static SubscriptionPayment payment(BillingInterval interval, Instant when) {
    return SubscriptionPayment.builder()
        .id(randomUUID().toString())
        .billingInterval(interval)
        .amountInCentsWithoutVat(1000L)
        .amountInCentsWithVat(1200L)
        .paymentDatetime(when)
        .invoiceId("inv-" + randomUUID())
        .build();
  }

  private static CreditTransaction consumption(
      String userId, CreditTransactionType type, long credits, Instant when) {
    return CreditTransaction.builder()
        .id(randomUUID().toString())
        .userId(userId)
        .type(type)
        .credits(credits)
        .creationDatetime(when)
        .build();
  }

  private static CreditTransaction grant(
      String userId, String planId, LocalDate periodStart, long credits) {
    return CreditTransaction.builder()
        .id(randomUUID().toString())
        .userId(userId)
        .type(SUBSCRIPTION_GRANT)
        .credits(credits)
        .subscriptionProductId(planId)
        .grantPeriodStart(periodStart)
        .build();
  }

  private static HInvoice paidInvoice(
      Instant createdDatetime, int quantity, String unitPrice, String vatPercent) {
    return HInvoice.builder()
        .id(randomUUID().toString())
        .createdDatetime(createdDatetime)
        .products(
            List.of(
                HInvoiceProduct.builder()
                    .id(randomUUID().toString())
                    .quantity(quantity)
                    .unitPrice(unitPrice)
                    .vatPercent(vatPercent)
                    .build()))
        .build();
  }
}
