package app.bpartners.api.service.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.rest.model.UserSubscriptionCommitmentDuration;
import app.bpartners.api.model.UserSubscriptionCommitment;
import app.bpartners.api.model.subscription.BillingInterval;
import app.bpartners.api.model.subscription.SubscriptionPayment;
import app.bpartners.api.payment.UserSubscriptionConf;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.UserSubscriptionCommitmentJpaRepository;
import app.bpartners.api.repository.jpa.SubscriptionInvoicePeriodRepository;
import app.bpartners.api.repository.jpa.SubscriptionPaymentRepository;
import app.bpartners.api.service.customer.SubscriptionCustomerResolver;
import app.bpartners.api.service.invoice.InvoiceService;
import app.bpartners.api.service.subscription.SubscriptionPaymentService;
import app.bpartners.api.service.utils.CustomDateFormatter;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class SubscriptionPaymentInvoiceRequestedServiceBackfillPreviewTest {
  private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
  private static final String SUBSCRIBER_ID = "subscriber_id";
  private static final LocalDate SUBSCRIBED_ON = LocalDate.of(2026, 9, 8);

  SubscriptionPaymentRepository subscriptionPaymentRepository = mock();
  SubscriptionPaymentService subscriptionPaymentService = mock();
  UserRepository userRepository = mock();
  UserSubscriptionCommitmentJpaRepository userSubscriptionCommitmentRepository = mock();
  UserSubscriptionConf userSubscriptionConf = mock();
  SubscriptionCustomerResolver subscriptionCustomerResolver = mock();
  InvoiceService invoiceService = mock();
  EventProducer eventProducer = mock();
  SubscriptionInvoicePeriodRepository subscriptionInvoicePeriodRepository = mock();

  SubscriptionPaymentInvoiceRequestedService subject =
      new SubscriptionPaymentInvoiceRequestedService(
          subscriptionPaymentRepository,
          subscriptionInvoicePeriodRepository,
          subscriptionPaymentService,
          userRepository,
          userSubscriptionCommitmentRepository,
          userSubscriptionConf,
          subscriptionCustomerResolver,
          invoiceService,
          new CustomDateFormatter(),
          eventProducer);

  SubscriptionPaymentInvoiceRequestedServiceBackfillPreviewTest() {
    when(userSubscriptionCommitmentRepository.findAllByUserId(SUBSCRIBER_ID))
        .thenReturn(List.of(twelveMonthsCommitmentFrom(SUBSCRIBED_ON)));
    when(subscriptionInvoicePeriodRepository.findByUserId(SUBSCRIBER_ID)).thenReturn(List.of());
    when(subscriptionPaymentRepository.findRefundedInvoiceIdsByUserId(SUBSCRIBER_ID))
        .thenReturn(List.of());
  }

  @Test
  void first_backfilled_payment_is_invoiced_on_its_own_prorated_period() {
    var septemberPayment =
        monthlyPayment(
            LocalDate.of(2026, 9, 8), LocalDate.of(2026, 9, 8), LocalDate.of(2026, 9, 30));

    var preview = subject.previewOwnPaidPeriod(septemberPayment);

    assertEquals(LocalDate.of(2026, 9, 8), preview.coversFrom());
    assertEquals(LocalDate.of(2026, 9, 30), preview.coversTo());
    assertEquals(1, preview.lineCount());
    assertFalse(preview.alreadyCovered());
  }

  @Test
  void next_backfilled_payment_assembles_the_commitment_months_left_after_september() {
    var octoberPayment =
        monthlyPayment(
            LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 30));

    var preview = subject.previewAssembledPeriod(octoberPayment, LocalDate.of(2026, 9, 30), null);

    assertEquals(LocalDate.of(2026, 10, 1), preview.coversFrom());
    assertEquals(LocalDate.of(2027, 8, 31), preview.coversTo());
    assertEquals(11, preview.lineCount());
    assertFalse(preview.alreadyCovered());
  }

  @Test
  void next_backfilled_payment_assembles_a_full_year_when_no_commitment_is_recorded() {
    when(userSubscriptionCommitmentRepository.findAllByUserId(SUBSCRIBER_ID)).thenReturn(List.of());
    var octoberPayment =
        monthlyPayment(
            LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 30));

    var preview = subject.previewAssembledPeriod(octoberPayment, null, null);

    assertEquals(LocalDate.of(2026, 10, 1), preview.coversFrom());
    assertEquals(LocalDate.of(2027, 9, 30), preview.coversTo());
    assertEquals(12, preview.lineCount());
  }

  @Test
  void a_commitment_planned_but_not_yet_written_shapes_the_simulated_period() {
    when(userSubscriptionCommitmentRepository.findAllByUserId(SUBSCRIBER_ID)).thenReturn(List.of());
    var octoberPayment =
        monthlyPayment(
            LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 30));

    var preview = subject.previewAssembledPeriod(octoberPayment, null, LocalDate.of(2027, 9, 28));

    assertEquals(LocalDate.of(2026, 10, 1), preview.coversFrom());
    assertEquals(LocalDate.of(2027, 8, 31), preview.coversTo());
    assertEquals(11, preview.lineCount());
  }

  @Test
  void a_recorded_commitment_wins_over_the_simulated_one() {
    var octoberPayment =
        monthlyPayment(
            LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 30));

    var preview = subject.previewAssembledPeriod(octoberPayment, null, LocalDate.of(2030, 1, 1));

    assertEquals(LocalDate.of(2027, 8, 31), preview.coversTo());
    assertEquals(11, preview.lineCount());
  }

  private UserSubscriptionCommitment twelveMonthsCommitmentFrom(LocalDate start) {
    return UserSubscriptionCommitment.builder()
        .id("commitment_id")
        .userId(SUBSCRIBER_ID)
        .duration(UserSubscriptionCommitmentDuration.TWELVE_MONTHS)
        .commitmentStartDatetime(atParisStartOfDay(start))
        .commitmentEndDatetime(atParisStartOfDay(start.plusYears(1)))
        .creationDatetime(atParisStartOfDay(start))
        .build();
  }

  private SubscriptionPayment monthlyPayment(
      LocalDate paidOn, LocalDate periodStart, LocalDate periodEnd) {
    return SubscriptionPayment.builder()
        .id("payment_of_" + periodStart)
        .userId(SUBSCRIBER_ID)
        .billingInterval(BillingInterval.MONTHLY)
        .amountInCentsWithoutVat(4900L)
        .amountInCentsWithVat(5880L)
        .vatPercent(2000L)
        .paymentDatetime(atParisStartOfDay(paidOn))
        .periodStartDatetime(atParisStartOfDay(periodStart))
        .periodEndDatetime(atParisStartOfDay(periodEnd))
        .build();
  }

  private static Instant atParisStartOfDay(LocalDate date) {
    return date.atStartOfDay(PARIS).toInstant();
  }
}
