package app.bpartners.api.service.event;

import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;
import static app.bpartners.api.service.utils.FractionUtils.parseFraction;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.SubscriptionPaymentInvoiceCreated;
import app.bpartners.api.endpoint.event.model.SubscriptionPaymentInvoiceRequested;
import app.bpartners.api.endpoint.rest.model.ArchiveStatus;
import app.bpartners.api.endpoint.rest.model.Invoice.PaymentTypeEnum;
import app.bpartners.api.endpoint.rest.model.PaymentMethod;
import app.bpartners.api.model.Customer;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.User;
import app.bpartners.api.model.UserSubscriptionCommitment;
import app.bpartners.api.model.subscription.AnnualInvoiceBillingType;
import app.bpartners.api.model.subscription.BillingInterval;
import app.bpartners.api.model.subscription.SubscriptionInvoicePeriod;
import app.bpartners.api.model.subscription.SubscriptionPayment;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.payment.UserSubscriptionConf;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.UserSubscriptionCommitmentJpaRepository;
import app.bpartners.api.repository.jpa.SubscriptionInvoicePeriodRepository;
import app.bpartners.api.repository.jpa.SubscriptionPaymentRepository;
import app.bpartners.api.service.customer.SubscriptionCustomerResolver;
import app.bpartners.api.service.invoice.InvoiceService;
import app.bpartners.api.service.subscription.SubscriptionPaymentService;
import app.bpartners.api.service.utils.CustomDateFormatter;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SubscriptionPaymentInvoiceRequestedServiceTest {
  private static final String ADMIN_USER_ID = "admin_user_id";
  private static final String PAYMENT_ID = "subscription_payment_id";
  private static final Instant PAID_AT = Instant.parse("2026-03-04T09:30:00Z");
  private static final Instant PERIOD_START = Instant.parse("2026-03-04T09:30:00Z");
  private static final Instant PERIOD_END = Instant.parse("2026-04-04T09:30:00Z");

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

  SubscriptionPaymentInvoiceRequestedServiceTest() {
    when(userSubscriptionConf.getUserToCreditId()).thenReturn(ADMIN_USER_ID);
    when(invoiceService.crupdateSubscriptionInvoice(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  @AfterEach
  void resetBillingType() {
    SubscriptionPaymentInvoiceRequestedService.annualInvoiceBillingType =
        AnnualInvoiceBillingType.MONTHLY_DETAILED;
    SubscriptionPaymentInvoiceRequestedService.excludesAlreadyInvoicedPeriods = true;
  }

  @Test
  void issues_an_admin_invoice_already_paid_for_the_subscriber() {
    var adminUser = User.builder().id(ADMIN_USER_ID).build();
    var subscriber = User.builder().id("subscriber_id").email("subscriber@email.com").build();
    var subscriberAsCustomer = Customer.builder().id("customer_id").name("Buyer SARL").build();
    givenUsers(adminUser, subscriber);
    when(subscriptionCustomerResolver.apply(adminUser, subscriber))
        .thenReturn(subscriberAsCustomer);
    givenPayment(somePayment().build());

    subject.accept(someEvent());

    var invoice = capturedInvoice();
    assertEquals(adminUser, invoice.getUser());
    assertEquals(subscriberAsCustomer, invoice.getCustomer());
    assertTrue(invoice.isSubscriptionInvoice());
    assertEquals(PAID, invoice.getStatus());
    assertNull(invoice.getValidityDate());
    assertEquals(ArchiveStatus.ENABLED, invoice.getArchiveStatus());
    assertEquals(PaymentMethod.CREDIT_CARD, invoice.getPaymentMethod());
    assertEquals(PaymentTypeEnum.CASH, invoice.getPaymentType());
    assertNotNull(invoice.getRef());
  }

  @Test
  void dates_and_references_the_invoice_on_the_payment_datetime() {
    givenDefaultUsersAndCustomer();
    givenPayment(somePayment().build());

    subject.accept(someEvent());

    var invoice = capturedInvoice();
    assertEquals("REF-04032026103000", invoice.getRef());
    assertEquals(LocalDate.of(2026, 3, 4), invoice.getSendingDate());
    assertEquals(LocalDate.of(2026, 3, 4), invoice.getToPayAt());
  }

  @Test
  void monthly_commitment_bills_every_month_until_the_end_of_the_commitment() {
    givenDefaultUsersAndCustomer();
    givenPayment(monthlyCommitmentPayment().build());

    subject.accept(someEvent());

    var invoice = capturedInvoice();
    var products = invoice.getProducts();
    assertEquals(12, products.size());
    assertEquals(
        "Abonnement Essentiel du 15/09/2026 au 30/09/2026", products.getFirst().getDescription());
    assertEquals(
        "Abonnement Essentiel du 01/10/2026 au 31/10/2026", products.get(1).getDescription());
    assertEquals(
        "Abonnement Essentiel du 01/08/2027 au 31/08/2027", products.getLast().getDescription());
    assertEquals(
        "Facture d'abonnement pour la période du 15/09/2026 au 31/08/2027", invoice.getTitle());
  }

  @Test
  void monthly_commitment_started_on_the_last_day_of_a_month_stops_on_the_last_full_month() {
    givenDefaultUsersAndCustomer();
    when(userSubscriptionCommitmentRepository.findAllByUserId("subscriber_id"))
        .thenReturn(
            List.of(
                UserSubscriptionCommitment.builder()
                    .commitmentStartDatetime(Instant.parse("2026-09-30T08:00:00Z"))
                    .commitmentEndDatetime(Instant.parse("2027-09-30T08:00:00Z"))
                    .build()));
    givenPayment(
        monthlyCommitmentPayment()
            .periodStartDatetime(Instant.parse("2026-09-30T08:00:00Z"))
            .periodEndDatetime(Instant.parse("2026-10-29T08:00:00Z"))
            .paymentDatetime(Instant.parse("2026-09-30T08:00:00Z"))
            .build());

    subject.accept(someEvent());

    var invoice = capturedInvoice();
    var products = invoice.getProducts();
    assertEquals(12, products.size());
    assertEquals(
        "Abonnement Essentiel du 30/09/2026 au 30/09/2026", products.getFirst().getDescription());
    assertEquals(
        "Abonnement Essentiel du 01/10/2026 au 31/10/2026", products.get(1).getDescription());
    assertEquals(
        "Abonnement Essentiel du 01/08/2027 au 31/08/2027", products.getLast().getDescription());
    assertEquals(
        "Facture d'abonnement pour la période du 30/09/2026 au 31/08/2027", invoice.getTitle());
  }

  @Test
  void monthly_commitment_ending_on_a_first_of_month_bills_that_month_in_full() {
    givenDefaultUsersAndCustomer();
    when(userSubscriptionCommitmentRepository.findAllByUserId("subscriber_id"))
        .thenReturn(
            List.of(
                UserSubscriptionCommitment.builder()
                    .commitmentStartDatetime(Instant.parse("2026-10-01T08:00:00Z"))
                    .commitmentEndDatetime(Instant.parse("2027-10-01T08:00:00Z"))
                    .build()));
    givenPayment(
        monthlyCommitmentPayment()
            .periodStartDatetime(Instant.parse("2026-10-01T08:00:00Z"))
            .periodEndDatetime(Instant.parse("2026-10-31T08:00:00Z"))
            .paymentDatetime(Instant.parse("2026-10-01T08:00:00Z"))
            .build());

    subject.accept(someEvent());

    var products = capturedInvoice().getProducts();
    assertEquals(12, products.size());
    assertEquals(
        "Abonnement Essentiel du 01/10/2026 au 31/10/2026", products.getFirst().getDescription());
    assertEquals(
        "Abonnement Essentiel du 01/09/2027 au 30/09/2027", products.getLast().getDescription());
  }

  @Test
  void monthly_commitment_bills_the_first_month_at_the_amount_charged_by_stripe() {
    givenDefaultUsersAndCustomer();
    givenPayment(monthlyCommitmentPayment().build());

    subject.accept(someEvent());

    var products = capturedInvoice().getProducts();
    assertEquals(parseFraction(2613), products.getFirst().getUnitPrice());
    assertEquals(parseFraction(4900), products.get(1).getUnitPrice());
    assertEquals(parseFraction(4900), products.getLast().getUnitPrice());
  }

  @Test
  void monthly_commitment_totals_the_amount_charged_by_stripe_for_the_first_month() {
    givenDefaultUsersAndCustomer();
    givenPayment(monthlyCommitmentPayment().build());

    subject.accept(someEvent());

    var invoice = capturedInvoice();
    assertEquals(565.13, invoice.getTotalPriceWithoutDiscount().getCentsAsDecimal(), 0.001);
    assertEquals(565.13, invoice.getTotalPriceWithoutVat().getCentsAsDecimal(), 0.001);
    assertEquals(113.03, invoice.getTotalVat().getCentsAsDecimal(), 0.001);
    assertEquals(678.16, invoice.getTotalPriceWithVat().getCentsAsDecimal(), 0.001);
  }

  @Test
  void monthly_commitment_prorates_the_first_month_on_days_when_stripe_charged_a_full_month() {
    givenDefaultUsersAndCustomer();
    givenPayment(
        monthlyCommitmentPayment()
            .amountInCentsWithoutVat(4_900L)
            .amountInCentsWithVat(5_880L)
            .build());

    subject.accept(someEvent());

    var products = capturedInvoice().getProducts();
    assertEquals(4900.0 * 16 / 30, products.getFirst().getUnitPrice().getApproximatedValue(), 0.01);
    assertEquals(parseFraction(4900), products.get(1).getUnitPrice());
  }

  @Test
  void monthly_commitment_prorates_the_first_month_on_days_when_no_amount_was_charged() {
    givenDefaultUsersAndCustomer();
    givenPayment(
        monthlyCommitmentPayment()
            .amountInCentsWithoutVat(null)
            .amountInCentsWithVat(null)
            .build());

    subject.accept(someEvent());

    var products = capturedInvoice().getProducts();
    assertEquals(4900.0 * 16 / 30, products.getFirst().getUnitPrice().getApproximatedValue(), 0.01);
    assertEquals(parseFraction(4900), products.get(1).getUnitPrice());
  }

  @Test
  void monthly_commitment_is_unpaid_payable_in_instalments_and_carries_no_discount() {
    givenDefaultUsersAndCustomer();
    givenPayment(monthlyCommitmentPayment().build());

    subject.accept(someEvent());

    var invoice = capturedInvoice();
    assertEquals(CONFIRMED, invoice.getStatus());
    assertEquals(PaymentTypeEnum.IN_INSTALMENT, invoice.getPaymentType());
    assertEquals(parseFraction(0), invoice.getDiscount().getPercentValue());
    assertEquals(LocalDate.of(2026, 9, 15), invoice.getToPayAt());
    assertTrue(invoice.isMonthlySubscriptionInvoice());
  }

  @Test
  void monthly_commitment_starting_on_the_first_bills_twelve_full_months() {
    givenDefaultUsersAndCustomer();
    givenPayment(
        monthlyCommitmentPayment()
            .periodStartDatetime(Instant.parse("2026-09-01T09:30:00Z"))
            .build());

    subject.accept(someEvent());

    var products = capturedInvoice().getProducts();
    assertEquals(12, products.size());
    assertEquals(
        "Abonnement Essentiel du 01/09/2026 au 30/09/2026", products.getFirst().getDescription());
    assertEquals(
        "Abonnement Essentiel du 01/08/2027 au 31/08/2027", products.getLast().getDescription());
    assertEquals(parseFraction(4900), products.getFirst().getUnitPrice());
  }

  @Test
  void monthly_commitment_ends_on_the_last_full_month_before_the_recorded_commitment_end() {
    givenDefaultUsersAndCustomer();
    when(userSubscriptionCommitmentRepository.findAllByUserId("subscriber_id"))
        .thenReturn(
            List.of(
                UserSubscriptionCommitment.builder()
                    .commitmentEndDatetime(Instant.parse("2027-09-14T09:30:00Z"))
                    .build()));
    givenPayment(monthlyCommitmentPayment().build());

    subject.accept(someEvent());

    var products = capturedInvoice().getProducts();
    assertEquals(
        "Abonnement Essentiel du 01/08/2027 au 31/08/2027", products.getLast().getDescription());
  }

  @Test
  void monthly_commitment_reads_the_end_of_the_most_recent_commitment() {
    givenDefaultUsersAndCustomer();
    when(userSubscriptionCommitmentRepository.findAllByUserId("subscriber_id"))
        .thenReturn(
            List.of(
                UserSubscriptionCommitment.builder()
                    .commitmentStartDatetime(Instant.parse("2025-11-01T00:00:00Z"))
                    .commitmentEndDatetime(Instant.parse("2026-10-31T12:00:00Z"))
                    .build(),
                UserSubscriptionCommitment.builder()
                    .commitmentStartDatetime(Instant.parse("2026-09-15T09:30:00Z"))
                    .commitmentEndDatetime(Instant.parse("2027-09-14T09:30:00Z"))
                    .build()));
    givenPayment(monthlyCommitmentPayment().build());

    subject.accept(someEvent());

    var products = capturedInvoice().getProducts();
    assertEquals(12, products.size());
    assertEquals(
        "Abonnement Essentiel du 01/08/2027 au 31/08/2027", products.getLast().getDescription());
  }

  @Test
  void monthly_commitment_skips_the_periods_already_invoiced() {
    givenSeptemberAlreadyInvoicedOfACommitmentStartedInSeptember();

    subject.accept(someEvent());

    var invoice = capturedInvoice();
    assertEquals(11, invoice.getProducts().size());
    assertEquals(
        "Abonnement Essentiel du 01/10/2026 au 31/10/2026",
        invoice.getProducts().getFirst().getDescription());
    assertEquals(parseFraction(4900), invoice.getProducts().getFirst().getUnitPrice());
    assertEquals(
        "Abonnement Essentiel du 01/08/2027 au 31/08/2027",
        invoice.getProducts().getLast().getDescription());
    assertEquals(
        "Facture d'abonnement pour la période du 01/10/2026 au 31/08/2027", invoice.getTitle());
  }

  @Test
  void october_payment_is_invoiced_for_the_period_left_after_the_september_invoice() {
    givenDefaultUsersAndCustomer();
    when(userSubscriptionCommitmentRepository.findAllByUserId("subscriber_id"))
        .thenReturn(
            List.of(
                UserSubscriptionCommitment.builder()
                    .commitmentStartDatetime(Instant.parse("2026-09-01T09:30:00Z"))
                    .commitmentEndDatetime(Instant.parse("2027-09-01T09:30:00Z"))
                    .build()));
    givenInvoicedPeriods(
        invoicedPeriod(
            "september_invoice_id", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)));
    var octoberPayment =
        monthlyCommitmentPayment()
            .periodStartDatetime(Instant.parse("2026-10-01T09:30:00Z"))
            .periodEndDatetime(Instant.parse("2026-10-31T09:30:00Z"))
            .paymentDatetime(Instant.parse("2026-10-01T09:30:00Z"))
            .build();
    givenPayment(octoberPayment);

    subject.accept(someEvent());

    var invoice = capturedInvoice();
    assertEquals(11, invoice.getProducts().size());
    assertEquals(
        "Abonnement Essentiel du 01/10/2026 au 31/10/2026",
        invoice.getProducts().getFirst().getDescription());
    assertEquals(
        "Abonnement Essentiel du 01/08/2027 au 31/08/2027",
        invoice.getProducts().getLast().getDescription());
    assertEquals(
        "Facture d'abonnement pour la période du 01/10/2026 au 31/08/2027", invoice.getTitle());
    verify(subscriptionPaymentService)
        .invoicedBy(
            octoberPayment,
            invoice.getId(),
            parisStartOfDay(LocalDate.of(2026, 10, 1)),
            parisStartOfDay(LocalDate.of(2027, 8, 31)));
    assertEquals(invoice.getId(), capturedCreatedEvent().getInvoiceId());
  }

  @Test
  void records_the_period_the_created_invoice_covers_for_the_subscriber() {
    givenDefaultUsersAndCustomer();
    givenPayment(monthlyCommitmentPayment().build());

    subject.accept(someEvent());

    var invoice = capturedInvoice();
    var captor = ArgumentCaptor.forClass(SubscriptionInvoicePeriod.class);
    verify(subscriptionInvoicePeriodRepository).save(captor.capture());
    var savedPeriod = captor.getValue();
    assertEquals("subscriber_id", savedPeriod.getUserId());
    assertEquals(invoice.getId(), savedPeriod.getInvoiceId());
    assertEquals(parisStartOfDay(LocalDate.of(2026, 9, 15)), savedPeriod.getPeriodStartDatetime());
    assertEquals(parisStartOfDay(LocalDate.of(2027, 8, 31)), savedPeriod.getPeriodEndDatetime());
  }

  @Test
  void monthly_commitment_bills_the_whole_period_when_the_exclusion_is_turned_off() {
    SubscriptionPaymentInvoiceRequestedService.excludesAlreadyInvoicedPeriods = false;
    givenSeptemberAlreadyInvoicedOfACommitmentStartedInSeptember();

    subject.accept(someEvent());

    var invoice = capturedInvoice();
    assertEquals(12, invoice.getProducts().size());
    assertEquals(
        "Abonnement Essentiel du 15/09/2026 au 30/09/2026",
        invoice.getProducts().getFirst().getDescription());
    assertEquals(parseFraction(2613), invoice.getProducts().getFirst().getUnitPrice());
    assertEquals(
        "Abonnement Essentiel du 01/10/2026 au 31/10/2026",
        invoice.getProducts().get(1).getDescription());
    assertEquals(
        "Abonnement Essentiel du 01/08/2027 au 31/08/2027",
        invoice.getProducts().getLast().getDescription());
    assertEquals(
        "Facture d'abonnement pour la période du 15/09/2026 au 31/08/2027", invoice.getTitle());
  }

  private void givenSeptemberAlreadyInvoicedOfACommitmentStartedInSeptember() {
    givenDefaultUsersAndCustomer();
    when(userSubscriptionCommitmentRepository.findAllByUserId("subscriber_id"))
        .thenReturn(
            List.of(
                UserSubscriptionCommitment.builder()
                    .commitmentStartDatetime(Instant.parse("2026-09-15T09:30:00Z"))
                    .commitmentEndDatetime(Instant.parse("2027-09-14T09:30:00Z"))
                    .build()));
    givenInvoicedPeriods(
        invoicedPeriod(
            "september_invoice_id", LocalDate.of(2026, 9, 15), LocalDate.of(2026, 9, 30)));
    givenPayment(monthlyCommitmentPayment().build());
  }

  @Test
  void monthly_commitment_ignores_an_invoiced_period_running_past_the_commitment() {
    givenDefaultUsersAndCustomer();
    when(userSubscriptionCommitmentRepository.findAllByUserId("subscriber_id"))
        .thenReturn(
            List.of(
                UserSubscriptionCommitment.builder()
                    .commitmentStartDatetime(Instant.parse("2026-09-15T09:30:00Z"))
                    .commitmentEndDatetime(Instant.parse("2027-09-14T09:30:00Z"))
                    .build()));
    givenInvoicedPeriods(
        invoicedPeriod("yearly_invoice_id", LocalDate.of(2026, 9, 15), LocalDate.of(2027, 9, 14)));
    givenPayment(monthlyCommitmentPayment().build());

    subject.accept(someEvent());

    var invoice = capturedInvoice();
    assertEquals(12, invoice.getProducts().size());
    assertEquals(
        "Abonnement Essentiel du 15/09/2026 au 30/09/2026",
        invoice.getProducts().getFirst().getDescription());
    assertEquals(
        "Abonnement Essentiel du 01/08/2027 au 31/08/2027",
        invoice.getProducts().getLast().getDescription());
    assertEquals(
        "Facture d'abonnement pour la période du 15/09/2026 au 31/08/2027", invoice.getTitle());
  }

  @Test
  void monthly_commitment_ignores_a_refunded_already_invoiced_period() {
    givenDefaultUsersAndCustomer();
    when(userSubscriptionCommitmentRepository.findAllByUserId("subscriber_id"))
        .thenReturn(
            List.of(
                UserSubscriptionCommitment.builder()
                    .commitmentStartDatetime(Instant.parse("2026-09-15T09:30:00Z"))
                    .commitmentEndDatetime(Instant.parse("2027-09-14T09:30:00Z"))
                    .build()));
    givenInvoicedPeriods(
        invoicedPeriod(
            "september_invoice_id", LocalDate.of(2026, 9, 15), LocalDate.of(2026, 9, 30)));
    when(subscriptionPaymentRepository.findRefundedInvoiceIdsByUserId("subscriber_id"))
        .thenReturn(List.of("september_invoice_id"));
    givenPayment(monthlyCommitmentPayment().build());

    subject.accept(someEvent());

    var invoice = capturedInvoice();
    assertEquals(12, invoice.getProducts().size());
    assertEquals(
        "Abonnement Essentiel du 15/09/2026 au 30/09/2026",
        invoice.getProducts().getFirst().getDescription());
  }

  @Test
  void monthly_commitment_is_not_invoiced_again_when_the_whole_commitment_is_already_invoiced() {
    var subscriptionPayment = givenACommitmentAlreadyInvoicedUntilItsEnd();

    subject.accept(someEvent());

    verify(invoiceService, never()).crupdateSubscriptionInvoice(any());
    verify(eventProducer, never()).accept(anyList());
    verify(subscriptionPaymentService)
        .invoicedBy(
            subscriptionPayment,
            "schedule_invoice_id",
            parisStartOfDay(LocalDate.of(2026, 9, 15)),
            parisStartOfDay(LocalDate.of(2027, 8, 31)));
  }

  @Test
  void monthly_commitment_reads_the_period_covered_by_the_already_created_invoice() {
    givenDefaultUsersAndCustomer();
    when(userSubscriptionCommitmentRepository.findAllByUserId("subscriber_id"))
        .thenReturn(
            List.of(
                UserSubscriptionCommitment.builder()
                    .commitmentStartDatetime(Instant.parse("2026-09-15T09:30:00Z"))
                    .commitmentEndDatetime(Instant.parse("2027-09-14T09:30:00Z"))
                    .build()));
    givenInvoicedPeriods(
        invoicedPeriod(
            "commitment_invoice_id", LocalDate.of(2026, 9, 15), LocalDate.of(2027, 8, 31)));
    givenPayment(
        monthlyCommitmentPayment()
            .periodStartDatetime(Instant.parse("2026-10-15T09:30:00Z"))
            .periodEndDatetime(Instant.parse("2026-11-14T09:30:00Z"))
            .paymentDatetime(Instant.parse("2026-10-15T09:30:00Z"))
            .build());

    subject.accept(someEvent());

    verify(invoiceService, never()).crupdateSubscriptionInvoice(any());
    verify(eventProducer, never()).accept(anyList());
  }

  @Test
  void monthly_commitment_records_the_period_the_created_invoice_covers() {
    givenDefaultUsersAndCustomer();
    var subscriptionPayment = monthlyCommitmentPayment().build();
    givenPayment(subscriptionPayment);

    subject.accept(someEvent());

    var invoice = capturedInvoice();
    verify(subscriptionPaymentService)
        .invoicedBy(
            subscriptionPayment,
            invoice.getId(),
            parisStartOfDay(LocalDate.of(2026, 9, 15)),
            parisStartOfDay(LocalDate.of(2027, 8, 31)));
  }

  @Test
  void remaining_period_triggered_after_a_prorated_september_bills_october_to_august() {
    givenDefaultUsersAndCustomer();
    when(userSubscriptionCommitmentRepository.findAllByUserId("subscriber_id"))
        .thenReturn(
            List.of(
                UserSubscriptionCommitment.builder()
                    .commitmentStartDatetime(Instant.parse("2026-09-15T09:30:00Z"))
                    .commitmentEndDatetime(Instant.parse("2027-09-15T09:30:00Z"))
                    .build()));
    givenInvoicedPeriods(
        invoicedPeriod(
            "september_invoice_id", LocalDate.of(2026, 9, 15), LocalDate.of(2026, 9, 30)));
    var septemberPayment =
        monthlyCommitmentPayment()
            .id("september_payment_id")
            .invoiceId("september_invoice_id")
            .periodStartDatetime(Instant.parse("2026-09-15T09:30:00Z"))
            .periodEndDatetime(Instant.parse("2026-10-14T09:30:00Z"))
            .paymentDatetime(Instant.parse("2026-09-15T09:30:00Z"))
            .build();

    var issued = subject.invoiceRemainingCommitmentPeriod(septemberPayment);

    var invoice = capturedInvoice();
    assertEquals(invoice.getId(), issued.orElseThrow().getId());
    var products = invoice.getProducts();
    assertEquals(11, products.size());
    assertEquals(
        "Abonnement Essentiel du 01/10/2026 au 31/10/2026", products.getFirst().getDescription());
    assertEquals(
        "Abonnement Essentiel du 01/08/2027 au 31/08/2027", products.getLast().getDescription());
    assertEquals(parseFraction(4900), products.getFirst().getUnitPrice());
    assertEquals(parseFraction(4900), products.getLast().getUnitPrice());
    assertEquals(
        "Facture d'abonnement pour la période du 01/10/2026 au 31/08/2027", invoice.getTitle());
    assertEquals(539.0, invoice.getTotalPriceWithoutVat().getCentsAsDecimal());
    assertEquals(646.8, invoice.getTotalPriceWithVat().getCentsAsDecimal(), 0.001);
    assertEquals(CONFIRMED, invoice.getStatus());
    assertEquals(PaymentTypeEnum.IN_INSTALMENT, invoice.getPaymentType());
    assertEquals(LocalDate.of(2026, 10, 1), invoice.getToPayAt());
    verify(subscriptionPaymentService, never()).invoicedBy(any(), anyString(), any(), any());
    var captor = ArgumentCaptor.forClass(SubscriptionInvoicePeriod.class);
    verify(subscriptionInvoicePeriodRepository).save(captor.capture());
    assertEquals(
        parisStartOfDay(LocalDate.of(2026, 10, 1)), captor.getValue().getPeriodStartDatetime());
    assertEquals(
        parisStartOfDay(LocalDate.of(2027, 8, 31)), captor.getValue().getPeriodEndDatetime());
    assertEquals("september_payment_id", capturedCreatedEvent().getSubscriptionPaymentId());
  }

  @Test
  void remaining_period_after_a_prorated_last_day_of_september_also_stops_on_august() {
    givenDefaultUsersAndCustomer();
    when(userSubscriptionCommitmentRepository.findAllByUserId("subscriber_id"))
        .thenReturn(
            List.of(
                UserSubscriptionCommitment.builder()
                    .commitmentStartDatetime(Instant.parse("2026-09-30T08:00:00Z"))
                    .commitmentEndDatetime(Instant.parse("2027-09-30T08:00:00Z"))
                    .build()));
    givenInvoicedPeriods(
        invoicedPeriod(
            "september_invoice_id", LocalDate.of(2026, 9, 30), LocalDate.of(2026, 9, 30)));
    var septemberPayment =
        monthlyCommitmentPayment()
            .id("september_payment_id")
            .invoiceId("september_invoice_id")
            .periodStartDatetime(Instant.parse("2026-09-30T08:00:00Z"))
            .periodEndDatetime(Instant.parse("2026-10-29T08:00:00Z"))
            .paymentDatetime(Instant.parse("2026-09-30T08:00:00Z"))
            .build();

    var issued = subject.invoiceRemainingCommitmentPeriod(septemberPayment);

    var invoice = capturedInvoice();
    assertEquals(invoice.getId(), issued.orElseThrow().getId());
    var products = invoice.getProducts();
    assertEquals(11, products.size());
    assertEquals(
        "Abonnement Essentiel du 01/10/2026 au 31/10/2026", products.getFirst().getDescription());
    assertEquals(
        "Abonnement Essentiel du 01/08/2027 au 31/08/2027", products.getLast().getDescription());
    assertEquals(539.0, invoice.getTotalPriceWithoutVat().getCentsAsDecimal());
    assertEquals(
        "Facture d'abonnement pour la période du 01/10/2026 au 31/08/2027", invoice.getTitle());
  }

  @Test
  void remaining_period_invoices_nothing_from_a_payment_billing_a_period_after_its_payment() {
    givenDefaultUsersAndCustomer();
    when(userSubscriptionCommitmentRepository.findAllByUserId("subscriber_id"))
        .thenReturn(
            List.of(
                UserSubscriptionCommitment.builder()
                    .commitmentStartDatetime(Instant.parse("2026-09-01T09:30:00Z"))
                    .commitmentEndDatetime(Instant.parse("2027-09-01T09:30:00Z"))
                    .build()));
    var corruptedPayment =
        monthlyCommitmentPayment()
            .id("corrupted_payment_id")
            .invoiceId("some_invoice_id")
            .periodStartDatetime(Instant.parse("2027-09-29T09:30:00Z"))
            .periodEndDatetime(Instant.parse("2027-10-28T09:30:00Z"))
            .paymentDatetime(Instant.parse("2026-09-15T09:30:00Z"))
            .build();

    var issued = subject.invoiceRemainingCommitmentPeriod(corruptedPayment);

    assertTrue(issued.isEmpty());
    verify(invoiceService, never()).crupdateSubscriptionInvoice(any());
    verify(subscriptionInvoicePeriodRepository, never()).save(any());
    verify(eventProducer, never()).accept(any());
  }

  @Test
  void a_payment_billing_a_period_after_its_payment_falls_back_on_a_cash_invoice() {
    givenDefaultUsersAndCustomer();
    when(userSubscriptionCommitmentRepository.findAllByUserId("subscriber_id"))
        .thenReturn(
            List.of(
                UserSubscriptionCommitment.builder()
                    .commitmentStartDatetime(Instant.parse("2026-09-01T09:30:00Z"))
                    .commitmentEndDatetime(Instant.parse("2027-09-01T09:30:00Z"))
                    .build()));
    givenPayment(
        monthlyCommitmentPayment()
            .periodStartDatetime(Instant.parse("2027-09-29T09:30:00Z"))
            .periodEndDatetime(Instant.parse("2027-10-28T09:30:00Z"))
            .paymentDatetime(Instant.parse("2026-09-15T09:30:00Z"))
            .build());

    subject.accept(someEvent());

    var invoice = capturedInvoice();
    assertEquals(1, invoice.getProducts().size());
    assertEquals(PaymentTypeEnum.CASH, invoice.getPaymentType());
    assertEquals(PAID, invoice.getStatus());
    assertEquals("Facture d'abonnement du 15/09/2026", invoice.getTitle());
  }

  @Test
  void backfills_the_remaining_commitment_period_from_an_already_invoiced_payment() {
    givenDefaultUsersAndCustomer();
    when(userSubscriptionCommitmentRepository.findAllByUserId("subscriber_id"))
        .thenReturn(
            List.of(
                UserSubscriptionCommitment.builder()
                    .commitmentStartDatetime(Instant.parse("2026-09-01T09:30:00Z"))
                    .commitmentEndDatetime(Instant.parse("2027-09-01T09:30:00Z"))
                    .build()));
    givenInvoicedPeriods(
        invoicedPeriod(
            "september_invoice_id", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30)));
    var septemberPayment =
        monthlyCommitmentPayment()
            .id("september_payment_id")
            .invoiceId("september_invoice_id")
            .periodStartDatetime(Instant.parse("2026-09-01T09:30:00Z"))
            .periodEndDatetime(Instant.parse("2026-09-30T09:30:00Z"))
            .paymentDatetime(Instant.parse("2026-09-01T09:30:00Z"))
            .build();

    var backfilled = subject.invoiceRemainingCommitmentPeriod(septemberPayment);

    var invoice = capturedInvoice();
    assertEquals(invoice.getId(), backfilled.orElseThrow().getId());
    assertEquals(11, invoice.getProducts().size());
    assertEquals(
        "Abonnement Essentiel du 01/10/2026 au 31/10/2026",
        invoice.getProducts().getFirst().getDescription());
    assertEquals(
        "Abonnement Essentiel du 01/08/2027 au 31/08/2027",
        invoice.getProducts().getLast().getDescription());
    assertEquals(
        "Facture d'abonnement pour la période du 01/10/2026 au 31/08/2027", invoice.getTitle());
    assertEquals(LocalDate.of(2026, 10, 1), invoice.getToPayAt());
    verify(subscriptionPaymentService, never()).invoicedBy(any(), anyString(), any(), any());
    var captor = ArgumentCaptor.forClass(SubscriptionInvoicePeriod.class);
    verify(subscriptionInvoicePeriodRepository).save(captor.capture());
    assertEquals(
        parisStartOfDay(LocalDate.of(2026, 10, 1)), captor.getValue().getPeriodStartDatetime());
    assertEquals(
        parisStartOfDay(LocalDate.of(2027, 8, 31)), captor.getValue().getPeriodEndDatetime());
    assertEquals("september_payment_id", capturedCreatedEvent().getSubscriptionPaymentId());
    assertEquals(invoice.getId(), capturedCreatedEvent().getInvoiceId());
  }

  @Test
  void backfills_nothing_when_the_commitment_is_already_invoiced_until_its_end() {
    givenACommitmentAlreadyInvoicedUntilItsEnd();
    var coveredPayment =
        monthlyCommitmentPayment()
            .id("covered_payment_id")
            .invoiceId("schedule_invoice_id")
            .build();

    var backfilled = subject.invoiceRemainingCommitmentPeriod(coveredPayment);

    assertTrue(backfilled.isEmpty());
    verify(invoiceService, never()).crupdateSubscriptionInvoice(any());
    verify(subscriptionInvoicePeriodRepository, never()).save(any());
    verify(eventProducer, never()).accept(anyList());
  }

  private void givenInvoicedPeriods(SubscriptionInvoicePeriod... invoicedPeriods) {
    when(subscriptionInvoicePeriodRepository.findByUserId("subscriber_id"))
        .thenReturn(List.of(invoicedPeriods));
  }

  private SubscriptionInvoicePeriod invoicedPeriod(
      String invoiceId, LocalDate periodStart, LocalDate periodEnd) {
    return SubscriptionInvoicePeriod.builder()
        .userId("subscriber_id")
        .invoiceId(invoiceId)
        .periodStartDatetime(parisStartOfDay(periodStart))
        .periodEndDatetime(parisStartOfDay(periodEnd))
        .build();
  }

  private Instant parisStartOfDay(LocalDate date) {
    return date.atStartOfDay(ZoneId.of("Europe/Paris")).toInstant();
  }

  private SubscriptionPayment givenACommitmentAlreadyInvoicedUntilItsEnd() {
    givenDefaultUsersAndCustomer();
    when(userSubscriptionCommitmentRepository.findAllByUserId("subscriber_id"))
        .thenReturn(
            List.of(
                UserSubscriptionCommitment.builder()
                    .commitmentStartDatetime(Instant.parse("2026-09-15T09:30:00Z"))
                    .commitmentEndDatetime(Instant.parse("2027-09-14T09:30:00Z"))
                    .build()));
    givenInvoicedPeriods(
        invoicedPeriod(
            "schedule_invoice_id", LocalDate.of(2026, 9, 15), LocalDate.of(2027, 8, 31)));
    var subscriptionPayment = monthlyCommitmentPayment().build();
    givenPayment(subscriptionPayment);
    return subscriptionPayment;
  }

  @Test
  void monthly_commitment_title_spans_exactly_the_billed_lines() {
    givenDefaultUsersAndCustomer();
    when(userSubscriptionCommitmentRepository.findAllByUserId("subscriber_id"))
        .thenReturn(
            List.of(
                UserSubscriptionCommitment.builder()
                    .commitmentStartDatetime(Instant.parse("2026-09-15T09:30:00Z"))
                    .commitmentEndDatetime(Instant.parse("2026-09-20T09:30:00Z"))
                    .build()));
    givenPayment(monthlyCommitmentPayment().build());

    subject.accept(someEvent());

    var invoice = capturedInvoice();
    assertEquals(1, invoice.getProducts().size());
    assertEquals(
        "Abonnement Essentiel du 15/09/2026 au 30/09/2026",
        invoice.getProducts().getFirst().getDescription());
    assertEquals(
        "Facture d'abonnement pour la période du 15/09/2026 au 30/09/2026", invoice.getTitle());
  }

  @Test
  void monthly_commitment_ignores_an_older_period_recorded_afterwards() {
    givenDefaultUsersAndCustomer();
    when(userSubscriptionCommitmentRepository.findAllByUserId("subscriber_id"))
        .thenReturn(
            List.of(
                UserSubscriptionCommitment.builder()
                    .commitmentStartDatetime(Instant.parse("2026-09-15T09:30:00Z"))
                    .commitmentEndDatetime(Instant.parse("2027-09-14T09:30:00Z"))
                    .creationDatetime(Instant.parse("2026-09-15T09:30:00Z"))
                    .build(),
                UserSubscriptionCommitment.builder()
                    .commitmentStartDatetime(Instant.parse("2025-01-01T00:00:00Z"))
                    .commitmentEndDatetime(Instant.parse("2026-12-31T12:00:00Z"))
                    .creationDatetime(Instant.parse("2026-10-01T09:30:00Z"))
                    .build()));
    givenPayment(monthlyCommitmentPayment().build());

    subject.accept(someEvent());

    var products = capturedInvoice().getProducts();
    assertEquals(12, products.size());
    assertEquals(
        "Abonnement Essentiel du 01/08/2027 au 31/08/2027", products.getLast().getDescription());
  }

  @Test
  void monthly_commitment_prefers_the_last_recorded_row_when_two_cover_the_same_period() {
    givenDefaultUsersAndCustomer();
    when(userSubscriptionCommitmentRepository.findAllByUserId("subscriber_id"))
        .thenReturn(
            List.of(
                UserSubscriptionCommitment.builder()
                    .commitmentStartDatetime(Instant.parse("2026-09-15T09:30:00Z"))
                    .commitmentEndDatetime(Instant.parse("2027-09-14T09:30:00Z"))
                    .creationDatetime(Instant.parse("2026-09-15T09:30:00Z"))
                    .build(),
                UserSubscriptionCommitment.builder()
                    .commitmentStartDatetime(Instant.parse("2026-09-15T09:30:00Z"))
                    .commitmentEndDatetime(Instant.parse("2027-07-14T09:30:00Z"))
                    .creationDatetime(Instant.parse("2026-09-20T09:30:00Z"))
                    .build()));
    givenPayment(monthlyCommitmentPayment().build());

    subject.accept(someEvent());

    var products = capturedInvoice().getProducts();
    assertEquals(10, products.size());
    assertEquals(
        "Abonnement Essentiel du 01/06/2027 au 30/06/2027", products.getLast().getDescription());
  }

  @Test
  void monthly_commitment_falls_back_on_a_year_when_every_commitment_is_already_over() {
    givenDefaultUsersAndCustomer();
    when(userSubscriptionCommitmentRepository.findAllByUserId("subscriber_id"))
        .thenReturn(
            List.of(
                UserSubscriptionCommitment.builder()
                    .commitmentStartDatetime(Instant.parse("2024-09-01T00:00:00Z"))
                    .commitmentEndDatetime(Instant.parse("2025-08-31T22:00:00Z"))
                    .build()));
    givenPayment(monthlyCommitmentPayment().build());

    subject.accept(someEvent());

    var products = capturedInvoice().getProducts();
    assertEquals(12, products.size());
    assertEquals(
        "Abonnement Essentiel du 01/08/2027 au 31/08/2027", products.getLast().getDescription());
  }

  @Test
  void titles_and_describes_the_invoice_with_the_billed_period() {
    givenDefaultUsersAndCustomer();
    givenPayment(somePayment().build());

    subject.accept(someEvent());

    var invoice = capturedInvoice();
    assertEquals(
        "Facture d'abonnement pour la période du 04/03/2026 au 04/04/2026", invoice.getTitle());
    assertEquals("Essentiel", invoice.getProducts().getFirst().getDescription());
  }

  @Test
  void prices_the_invoice_line_on_the_amount_paid_without_vat() {
    givenDefaultUsersAndCustomer();
    givenPayment(somePayment().build());

    subject.accept(someEvent());

    var product = capturedInvoice().getProducts().getFirst();
    assertEquals(1, product.getQuantity());
    assertEquals(parseFraction(4083), product.getUnitPrice());
    assertEquals(parseFraction(2000), product.getVatPercent());
  }

  @Test
  void yearly_payment_bills_a_single_monthly_line_with_quantity_twelve_and_ten_percent_discount() {
    givenDefaultUsersAndCustomer();
    SubscriptionPaymentInvoiceRequestedService.annualInvoiceBillingType =
        AnnualInvoiceBillingType.MONTHLY_QUANTITY;
    givenPayment(someYearlyPayment().build());

    subject.accept(someEvent());

    var invoice = capturedInvoice();
    assertEquals(1, invoice.getProducts().size());
    var product = invoice.getProducts().getFirst();
    assertEquals(12, product.getQuantity());
    assertEquals("Abonnement Essentiel du 01/01/2026 au 31/12/2026", product.getDescription());
    assertEquals(100.0, product.getUnitPrice().getCentsAsDecimal());
    assertEquals(10.0, invoice.getDiscount().getPercentValue().getCentsAsDecimal());
    assertEquals(1200.0, invoice.getTotalPriceWithoutDiscount().getCentsAsDecimal());
    assertEquals(120.0, invoice.getDiscount().getAmountValue().getCentsAsDecimal());
    assertEquals(1080.0, invoice.getTotalPriceWithoutVat().getCentsAsDecimal());
    assertEquals(1296.0, invoice.getTotalPriceWithVat().getCentsAsDecimal());
  }

  @Test
  void yearly_payment_can_bill_twelve_detailed_monthly_lines_each_with_its_period() {
    givenDefaultUsersAndCustomer();
    SubscriptionPaymentInvoiceRequestedService.annualInvoiceBillingType =
        AnnualInvoiceBillingType.MONTHLY_DETAILED;
    givenPayment(someYearlyPayment().build());

    subject.accept(someEvent());

    var invoice = capturedInvoice();
    assertEquals(12, invoice.getProducts().size());
    assertTrue(invoice.getProducts().stream().allMatch(product -> product.getQuantity() == 1));
    assertEquals(
        "Abonnement Essentiel du 01/01/2026 au 31/01/2026",
        invoice.getProducts().getFirst().getDescription());
    assertEquals(
        "Abonnement Essentiel du 01/12/2026 au 31/12/2026",
        invoice.getProducts().getLast().getDescription());
    assertEquals(100.0, invoice.getProducts().getFirst().getUnitPrice().getCentsAsDecimal());
    assertEquals(10.0, invoice.getDiscount().getPercentValue().getCentsAsDecimal());
    assertEquals(1080.0, invoice.getTotalPriceWithoutVat().getCentsAsDecimal());
    assertEquals(1296.0, invoice.getTotalPriceWithVat().getCentsAsDecimal());
  }

  @Test
  void yearly_payment_prorates_the_first_and_last_month_on_calendar_months() {
    givenDefaultUsersAndCustomer();
    SubscriptionPaymentInvoiceRequestedService.annualInvoiceBillingType =
        AnnualInvoiceBillingType.MONTHLY_DETAILED;
    givenPayment(
        someYearlyPayment()
            .periodStartDatetime(Instant.parse("2026-09-15T09:30:00Z"))
            .periodEndDatetime(Instant.parse("2027-09-14T09:30:00Z"))
            .build());

    subject.accept(someEvent());

    var invoice = capturedInvoice();
    var products = invoice.getProducts();
    assertEquals(13, products.size());
    assertTrue(products.stream().allMatch(product -> product.getQuantity() == 1));
    assertEquals(
        "Abonnement Essentiel du 15/09/2026 au 30/09/2026", products.getFirst().getDescription());
    assertEquals(
        "Abonnement Essentiel du 01/10/2026 au 31/10/2026", products.get(1).getDescription());
    assertEquals(
        "Abonnement Essentiel du 01/09/2027 au 14/09/2027", products.getLast().getDescription());
    assertEquals(53.33, products.getFirst().getUnitPrice().getCentsAsDecimal());
    assertEquals(100.0, products.get(1).getUnitPrice().getCentsAsDecimal());
    assertEquals(46.67, products.getLast().getUnitPrice().getCentsAsDecimal());
    assertEquals(1200.0, invoice.getTotalPriceWithoutDiscount().getCentsAsDecimal());
    assertEquals(1080.0, invoice.getTotalPriceWithoutVat().getCentsAsDecimal());
    assertEquals(1296.0, invoice.getTotalPriceWithVat().getCentsAsDecimal());
  }

  @Test
  void yearly_payment_keeps_full_months_at_catalog_price_and_absorbs_rounding_on_the_last_line() {
    givenDefaultUsersAndCustomer();
    SubscriptionPaymentInvoiceRequestedService.annualInvoiceBillingType =
        AnnualInvoiceBillingType.MONTHLY_DETAILED;
    givenPayment(
        someYearlyPayment()
            .subscriptionProduct(
                SubscriptionProduct.builder()
                    .name("Essentiel")
                    .annualDiscountPercent(1000)
                    .priceInCentsWithoutVat(4_900L)
                    .build())
            .amountInCentsWithoutVat(52_900L)
            .amountInCentsWithVat(63_480L)
            .periodStartDatetime(Instant.parse("2026-09-15T09:30:00Z"))
            .periodEndDatetime(Instant.parse("2027-09-14T09:30:00Z"))
            .build());

    subject.accept(someEvent());

    var invoice = capturedInvoice();
    var products = invoice.getProducts();
    assertEquals(13, products.size());
    assertEquals(26.13, products.getFirst().getUnitPrice().getCentsAsDecimal());
    assertEquals(49.0, products.get(1).getUnitPrice().getCentsAsDecimal());
    assertEquals(22.64, products.getLast().getUnitPrice().getCentsAsDecimal());
    assertEquals(587.78, invoice.getTotalPriceWithoutDiscount().getCentsAsDecimal());
    assertEquals(529.0, invoice.getTotalPriceWithoutVat().getCentsAsDecimal());
    assertEquals(634.8, invoice.getTotalPriceWithVat().getCentsAsDecimal());
  }

  @Test
  void yearly_payment_prefers_the_declared_discount_over_the_catalog_price() {
    givenDefaultUsersAndCustomer();
    SubscriptionPaymentInvoiceRequestedService.annualInvoiceBillingType =
        AnnualInvoiceBillingType.MONTHLY_QUANTITY;
    givenPayment(
        someYearlyPayment()
            .subscriptionProduct(
                SubscriptionProduct.builder()
                    .name("Essentiel")
                    .annualDiscountPercent(1000)
                    .priceInCentsWithoutVat(11_000L)
                    .build())
            .build());

    subject.accept(someEvent());

    var invoice = capturedInvoice();
    var product = invoice.getProducts().getFirst();
    assertEquals(100.0, product.getUnitPrice().getCentsAsDecimal());
    assertEquals(10.0, invoice.getDiscount().getPercentValue().getCentsAsDecimal());
    assertEquals(1080.0, invoice.getTotalPriceWithoutVat().getCentsAsDecimal());
  }

  @Test
  void yearly_payment_reads_the_declared_discount_stored_in_hundredths_of_percent() {
    givenDefaultUsersAndCustomer();
    SubscriptionPaymentInvoiceRequestedService.annualInvoiceBillingType =
        AnnualInvoiceBillingType.MONTHLY_DETAILED;
    givenPayment(
        someYearlyPayment()
            .subscriptionProduct(
                SubscriptionProduct.builder()
                    .name("Essentiel")
                    .annualDiscountPercent(1000)
                    .priceInCentsWithoutVat(4_900L)
                    .build())
            .amountInCentsWithoutVat(52_900L)
            .amountInCentsWithVat(63_480L)
            .build());

    subject.accept(someEvent());

    var invoice = capturedInvoice();
    assertEquals(49.0, invoice.getProducts().getFirst().getUnitPrice().getCentsAsDecimal());
    assertEquals(48.78, invoice.getProducts().getLast().getUnitPrice().getCentsAsDecimal());
    assertEquals(10.0, invoice.getDiscount().getPercentValue().getCentsAsDecimal());
    assertEquals(529.0, invoice.getTotalPriceWithoutVat().getCentsAsDecimal());
    assertEquals(634.8, invoice.getTotalPriceWithVat().getCentsAsDecimal());
  }

  @Test
  void yearly_payment_reads_the_declared_discount_for_the_pro_plan() {
    givenDefaultUsersAndCustomer();
    SubscriptionPaymentInvoiceRequestedService.annualInvoiceBillingType =
        AnnualInvoiceBillingType.MONTHLY_DETAILED;
    givenPayment(
        someYearlyPayment()
            .subscriptionProduct(
                SubscriptionProduct.builder()
                    .name("Pro")
                    .annualDiscountPercent(1000)
                    .priceInCentsWithoutVat(9_900L)
                    .build())
            .amountInCentsWithoutVat(106_900L)
            .amountInCentsWithVat(128_280L)
            .build());

    subject.accept(someEvent());

    var invoice = capturedInvoice();
    assertEquals(99.0, invoice.getProducts().getFirst().getUnitPrice().getCentsAsDecimal());
    assertEquals(98.78, invoice.getProducts().getLast().getUnitPrice().getCentsAsDecimal());
    assertEquals(10.0, invoice.getDiscount().getPercentValue().getCentsAsDecimal());
    assertEquals(1069.0, invoice.getTotalPriceWithoutVat().getCentsAsDecimal());
    assertEquals(1282.8, invoice.getTotalPriceWithVat().getCentsAsDecimal());
  }

  @Test
  void yearly_payment_reads_the_declared_discount_for_the_expert_plan() {
    givenDefaultUsersAndCustomer();
    SubscriptionPaymentInvoiceRequestedService.annualInvoiceBillingType =
        AnnualInvoiceBillingType.MONTHLY_DETAILED;
    givenPayment(
        someYearlyPayment()
            .subscriptionProduct(
                SubscriptionProduct.builder()
                    .name("Expert")
                    .annualDiscountPercent(1000)
                    .priceInCentsWithoutVat(19_900L)
                    .build())
            .amountInCentsWithoutVat(214_900L)
            .amountInCentsWithVat(257_880L)
            .build());

    subject.accept(someEvent());

    var invoice = capturedInvoice();
    assertEquals(199.0, invoice.getProducts().getFirst().getUnitPrice().getCentsAsDecimal());
    assertEquals(198.78, invoice.getProducts().getLast().getUnitPrice().getCentsAsDecimal());
    assertEquals(10.0, invoice.getDiscount().getPercentValue().getCentsAsDecimal());
    assertEquals(2149.0, invoice.getTotalPriceWithoutVat().getCentsAsDecimal());
    assertEquals(2578.8, invoice.getTotalPriceWithVat().getCentsAsDecimal());
  }

  @Test
  void yearly_payment_reconstructs_the_discount_column_from_the_catalog_monthly_price() {
    givenDefaultUsersAndCustomer();
    SubscriptionPaymentInvoiceRequestedService.annualInvoiceBillingType =
        AnnualInvoiceBillingType.MONTHLY_DETAILED;
    givenPayment(yearlyPaymentWithoutDeclaredDiscount().build());

    subject.accept(someEvent());

    var invoice = capturedInvoice();
    assertEquals(12, invoice.getProducts().size());
    assertEquals(48.98, invoice.getProducts().getFirst().getUnitPrice().getCentsAsDecimal());
    assertEquals(10.0, invoice.getDiscount().getPercentValue().getCentsAsDecimal());
    assertEquals(587.76, invoice.getTotalPriceWithoutDiscount().getCentsAsDecimal());
    assertEquals(528.96, invoice.getTotalPriceWithoutVat().getCentsAsDecimal());
    assertEquals(634.75, invoice.getTotalPriceWithVat().getCentsAsDecimal());
  }

  @Test
  void titles_the_invoice_on_the_payment_date_when_the_period_is_unknown() {
    givenDefaultUsersAndCustomer();
    givenPayment(somePayment().periodStartDatetime(null).periodEndDatetime(null).build());

    subject.accept(someEvent());

    var invoice = capturedInvoice();
    assertEquals("Facture d'abonnement du 04/03/2026", invoice.getTitle());
    assertEquals("Essentiel", invoice.getProducts().getFirst().getDescription());
  }

  @Test
  void links_the_created_invoice_to_the_payment_and_notifies_the_subscriber() {
    givenDefaultUsersAndCustomer();
    var subscriptionPayment = somePayment().build();
    givenPayment(subscriptionPayment);

    subject.accept(someEvent());

    var invoice = capturedInvoice();
    verify(subscriptionPaymentService)
        .invoicedBy(eq(subscriptionPayment), eq(invoice.getId()), any(), any());
    var created = capturedCreatedEvent();
    assertEquals(invoice.getId(), created.getInvoiceId());
    assertEquals(PAYMENT_ID, created.getSubscriptionPaymentId());
  }

  @Test
  void does_not_invoice_an_already_invoiced_payment() {
    givenPayment(somePayment().invoiceId("invoice_id").build());

    subject.accept(someEvent());

    verify(invoiceService, never()).crupdateSubscriptionInvoice(any());
    verify(eventProducer, never()).accept(anyList());
  }

  @Test
  void does_not_invoice_an_unknown_payment() {
    when(subscriptionPaymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.empty());

    subject.accept(someEvent());

    verify(invoiceService, never()).crupdateSubscriptionInvoice(any());
    verify(eventProducer, never()).accept(anyList());
  }

  @Test
  void retries_the_invoicing_for_at_most_five_minutes() {
    var event = someEvent();

    assertEquals(Duration.ofMinutes(5L), event.maxConsumerDuration());
    assertEquals(Duration.ofMinutes(1L), event.maxConsumerBackoffBetweenRetries());
  }

  @Test
  void yearly_payment_without_period_dates_labels_the_grouped_line_with_the_plan_only() {
    givenDefaultUsersAndCustomer();
    SubscriptionPaymentInvoiceRequestedService.annualInvoiceBillingType =
        AnnualInvoiceBillingType.MONTHLY_QUANTITY;
    givenPayment(someYearlyPayment().periodStartDatetime(null).periodEndDatetime(null).build());

    subject.accept(someEvent());

    var invoice = capturedInvoice();
    assertEquals("Facture d'abonnement du 04/03/2026", invoice.getTitle());
    assertEquals("Abonnement Essentiel", invoice.getProducts().getFirst().getDescription());
  }

  @Test
  void yearly_payment_without_period_end_bills_the_twelve_months_after_the_start() {
    givenDefaultUsersAndCustomer();
    SubscriptionPaymentInvoiceRequestedService.annualInvoiceBillingType =
        AnnualInvoiceBillingType.MONTHLY_DETAILED;
    givenPayment(someYearlyPayment().periodEndDatetime(null).build());

    subject.accept(someEvent());

    var products = capturedInvoice().getProducts();
    assertEquals(12, products.size());
    assertEquals(
        "Abonnement Essentiel du 01/01/2026 au 31/01/2026", products.getFirst().getDescription());
    assertEquals(
        "Abonnement Essentiel du 01/12/2026 au 31/12/2026", products.getLast().getDescription());
    assertEquals(100.0, products.getFirst().getUnitPrice().getCentsAsDecimal());
  }

  @Test
  void yearly_payment_without_any_catalog_price_spreads_the_amount_paid_over_twelve_months() {
    givenDefaultUsersAndCustomer();
    SubscriptionPaymentInvoiceRequestedService.annualInvoiceBillingType =
        AnnualInvoiceBillingType.MONTHLY_QUANTITY;
    givenPayment(someYearlyPayment().subscriptionProduct(null).build());

    subject.accept(someEvent());

    var invoice = capturedInvoice();
    var product = invoice.getProducts().getFirst();
    assertEquals("Abonnement du 01/01/2026 au 31/12/2026", product.getDescription());
    assertEquals(12, product.getQuantity());
    assertEquals(90.0, product.getUnitPrice().getCentsAsDecimal());
    assertEquals(0.0, invoice.getDiscount().getPercentValue().getCentsAsDecimal());
    assertEquals(0.0, invoice.getDiscount().getAmountValue().getCentsAsDecimal());
    assertEquals(1080.0, invoice.getTotalPriceWithoutDiscount().getCentsAsDecimal());
    assertEquals(1080.0, invoice.getTotalPriceWithoutVat().getCentsAsDecimal());
    assertEquals(1296.0, invoice.getTotalPriceWithVat().getCentsAsDecimal());
  }

  @Test
  void yearly_payment_ignores_a_catalog_price_not_above_the_amount_paid() {
    givenDefaultUsersAndCustomer();
    SubscriptionPaymentInvoiceRequestedService.annualInvoiceBillingType =
        AnnualInvoiceBillingType.MONTHLY_QUANTITY;
    givenPayment(
        someYearlyPayment()
            .subscriptionProduct(
                SubscriptionProduct.builder()
                    .name("Essentiel")
                    .priceInCentsWithoutVat(4_000L)
                    .build())
            .build());

    subject.accept(someEvent());

    var invoice = capturedInvoice();
    var product = invoice.getProducts().getFirst();
    assertEquals(90.0, product.getUnitPrice().getCentsAsDecimal());
    assertEquals(0.0, invoice.getDiscount().getPercentValue().getCentsAsDecimal());
    assertEquals(1080.0, invoice.getTotalPriceWithoutVat().getCentsAsDecimal());
  }

  @Test
  void monthly_payment_is_not_discounted_even_when_the_plan_declares_an_annual_discount() {
    givenDefaultUsersAndCustomer();
    givenPayment(
        somePayment()
            .subscriptionProduct(
                SubscriptionProduct.builder()
                    .name("Essentiel")
                    .annualDiscountPercent(1000)
                    .priceInCentsWithoutVat(4_900L)
                    .build())
            .build());

    subject.accept(someEvent());

    var invoice = capturedInvoice();
    assertEquals(1, invoice.getProducts().size());
    assertEquals(0.0, invoice.getDiscount().getPercentValue().getCentsAsDecimal());
    assertEquals(40.83, invoice.getProducts().getFirst().getUnitPrice().getCentsAsDecimal());
  }

  private void givenDefaultUsersAndCustomer() {
    var adminUser = User.builder().id(ADMIN_USER_ID).build();
    var subscriber = User.builder().id("subscriber_id").email("subscriber@email.com").build();
    givenUsers(adminUser, subscriber);
    when(subscriptionCustomerResolver.apply(adminUser, subscriber))
        .thenReturn(Customer.builder().id("customer_id").name("Buyer SARL").build());
  }

  private void givenUsers(User adminUser, User subscriber) {
    when(userRepository.getById(ADMIN_USER_ID)).thenReturn(adminUser);
    when(userRepository.getById(subscriber.getId())).thenReturn(subscriber);
  }

  private void givenPayment(SubscriptionPayment subscriptionPayment) {
    when(subscriptionPaymentRepository.findById(PAYMENT_ID))
        .thenReturn(Optional.of(subscriptionPayment));
  }

  private SubscriptionPayment.SubscriptionPaymentBuilder somePayment() {
    return SubscriptionPayment.builder()
        .id(PAYMENT_ID)
        .userId("subscriber_id")
        .stripeInvoiceId("in_123")
        .label("Essentiel")
        .amountInCentsWithoutVat(4_083L)
        .amountInCentsWithVat(4_900L)
        .vatPercent(2_000L)
        .periodStartDatetime(PERIOD_START)
        .periodEndDatetime(PERIOD_END)
        .paymentDatetime(PAID_AT);
  }

  private SubscriptionPayment.SubscriptionPaymentBuilder monthlyCommitmentPayment() {
    return SubscriptionPayment.builder()
        .id(PAYMENT_ID)
        .userId("subscriber_id")
        .stripeInvoiceId("in_123")
        .label("Essentiel")
        .billingInterval(BillingInterval.MONTHLY)
        .subscriptionProduct(
            SubscriptionProduct.builder().name("Essentiel").priceInCentsWithoutVat(4_900L).build())
        .amountInCentsWithoutVat(2_613L)
        .amountInCentsWithVat(3_136L)
        .vatPercent(2_000L)
        .periodStartDatetime(Instant.parse("2026-09-15T09:30:00Z"))
        .periodEndDatetime(Instant.parse("2026-10-14T09:30:00Z"))
        .paymentDatetime(Instant.parse("2026-09-15T09:30:00Z"));
  }

  private SubscriptionPayment.SubscriptionPaymentBuilder someYearlyPayment() {
    return SubscriptionPayment.builder()
        .id(PAYMENT_ID)
        .userId("subscriber_id")
        .stripeInvoiceId("in_123")
        .label("Essentiel")
        .billingInterval(BillingInterval.YEARLY)
        .subscriptionProduct(
            SubscriptionProduct.builder().name("Essentiel").annualDiscountPercent(1000).build())
        .amountInCentsWithoutVat(108_000L)
        .amountInCentsWithVat(129_600L)
        .vatPercent(2_000L)
        .periodStartDatetime(Instant.parse("2026-01-01T09:30:00Z"))
        .periodEndDatetime(Instant.parse("2026-12-31T09:30:00Z"))
        .paymentDatetime(PAID_AT);
  }

  private SubscriptionPayment.SubscriptionPaymentBuilder yearlyPaymentWithoutDeclaredDiscount() {
    return SubscriptionPayment.builder()
        .id(PAYMENT_ID)
        .userId("subscriber_id")
        .stripeInvoiceId("in_123")
        .label("Essentiel")
        .billingInterval(BillingInterval.YEARLY)
        .subscriptionProduct(
            SubscriptionProduct.builder().name("Essentiel").priceInCentsWithoutVat(4_898L).build())
        .amountInCentsWithoutVat(52_896L)
        .amountInCentsWithVat(63_475L)
        .vatPercent(2_000L)
        .periodStartDatetime(Instant.parse("2026-01-01T09:30:00Z"))
        .periodEndDatetime(Instant.parse("2026-12-31T09:30:00Z"))
        .paymentDatetime(PAID_AT);
  }

  private SubscriptionPaymentInvoiceRequested someEvent() {
    return SubscriptionPaymentInvoiceRequested.builder().subscriptionPaymentId(PAYMENT_ID).build();
  }

  private Invoice capturedInvoice() {
    var captor = ArgumentCaptor.forClass(Invoice.class);
    verify(invoiceService).crupdateSubscriptionInvoice(captor.capture());
    return captor.getValue();
  }

  private SubscriptionPaymentInvoiceCreated capturedCreatedEvent() {
    var captor = ArgumentCaptor.forClass(List.class);
    verify(eventProducer).accept(captor.capture());
    return (SubscriptionPaymentInvoiceCreated) captor.getValue().getFirst();
  }
}
