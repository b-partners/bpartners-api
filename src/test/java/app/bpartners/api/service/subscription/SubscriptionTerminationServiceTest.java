package app.bpartners.api.service.subscription;

import static app.bpartners.api.model.subscription.SubscriptionTerminationStatus.CREDIT_NOTE_ISSUED;
import static app.bpartners.api.model.subscription.SubscriptionTerminationStatus.FAILED;
import static app.bpartners.api.model.subscription.SubscriptionTerminationStatus.PENDING;
import static app.bpartners.api.model.subscription.SubscriptionTerminationStatus.SKIPPED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.model.Customer;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.InvoiceDiscount;
import app.bpartners.api.model.InvoiceProduct;
import app.bpartners.api.model.User;
import app.bpartners.api.model.subscription.SubscriptionInvoicePeriod;
import app.bpartners.api.model.subscription.TempSubscriptionTermination;
import app.bpartners.api.payment.UserSubscriptionConf;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.jpa.TempSubscriptionTerminationJpaRepository;
import app.bpartners.api.service.customer.SubscriptionCustomerResolver;
import app.bpartners.api.service.subscription.SubscriptionTerminationService.BilledLine;
import app.bpartners.api.service.utils.CustomDateFormatter;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SubscriptionTerminationServiceTest {
  private static final String TERMINATION_ID = "termination_id";
  private static final String USER_ID = "subscriber_id";
  private static final String E2_ID = "cus_1";
  private static final String BIRDIA_ID = "birdia_id";
  private static final Instant END = Instant.parse("2026-11-10T23:00:00Z");

  TempSubscriptionTerminationJpaRepository repository = mock();
  UserRepository userRepository = mock();
  UserSubscriptionProductService userSubscriptionProductService = mock();
  LiveSubscriptionInvoicePeriodFinder livePeriodFinder = mock();
  InvoiceRepository invoiceRepository = mock();
  SubscriptionCustomerResolver subscriptionCustomerResolver = mock();
  SubscriptionUpgradeInvoicingService invoicingService = mock();
  CustomDateFormatter customDateFormatter = new CustomDateFormatter();

  SubscriptionTerminationService subject =
      new SubscriptionTerminationService(
          repository,
          userRepository,
          userSubscriptionProductService,
          livePeriodFinder,
          invoiceRepository,
          new UserSubscriptionConf(BIRDIA_ID),
          subscriptionCustomerResolver,
          new SubscriptionUpgradeInvoiceLines(customDateFormatter),
          invoicingService,
          customDateFormatter);

  User subscriber = User.builder().id(USER_ID).build();
  User birdia = User.builder().id(BIRDIA_ID).build();
  Customer customer = Customer.builder().id("customer_id").build();

  @BeforeEach
  void setUp() {
    when(repository.save(any(TempSubscriptionTermination.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(userRepository.findByStripeCustomerId(E2_ID)).thenReturn(Optional.of(subscriber));
    when(userRepository.getById(BIRDIA_ID)).thenReturn(birdia);
    when(subscriptionCustomerResolver.apply(birdia, subscriber)).thenReturn(customer);
  }

  @Test
  void credits_only_months_starting_after_the_month_of_current_period_end() {
    var lines =
        List.of(billedLine("2026-11-01", "2026-11-30"), billedLine("2026-12-01", "2026-12-31"));

    var credited =
        SubscriptionTerminationService.linesToCredit(LocalDate.parse("2026-11-30"), lines);

    assertEquals(1, credited.size());
    assertEquals(LocalDate.parse("2026-12-01"), credited.getFirst().start());
  }

  @Test
  void credits_from_first_of_next_month_when_current_period_end_is_not_a_month_end() {
    var lines =
        List.of(billedLine("2026-10-01", "2026-10-31"), billedLine("2026-11-01", "2026-11-30"));

    var credited =
        SubscriptionTerminationService.linesToCredit(LocalDate.parse("2026-10-30"), lines);

    assertEquals(List.of(lines.get(1)), credited);
  }

  @Test
  void amount_with_vat_of_billed_line_is_computed_in_cents() {
    assertEquals(
        5880L,
        SubscriptionTerminationService.amountInCentsWithVatOf(product("2026-11-01", "2026-11-30")));
  }

  @Test
  void check_fails_when_no_line_covers_current_period_end() {
    var termination = termination("2026-11-30", 5880);

    assertThrows(
        IllegalStateException.class,
        () ->
            SubscriptionTerminationService.checkBilledAmount(
                termination, List.of(billedLine("2026-12-01", "2026-12-31"))));
  }

  @Test
  void check_fails_when_covering_line_amount_differs() {
    var termination = termination("2026-11-30", 9999);

    assertThrows(
        IllegalStateException.class,
        () ->
            SubscriptionTerminationService.checkBilledAmount(
                termination, List.of(billedLine("2026-11-01", "2026-11-30"))));
  }

  @Test
  void terminate_ends_subscription_and_issues_credit_note_without_any_mail() {
    var termination = termination("2026-11-30", 5880);
    when(repository.findById(TERMINATION_ID)).thenReturn(Optional.of(termination));
    var invoice = invoiceWithMonths("2026-11-01", "2026-12-01");
    when(livePeriodFinder.findLive(eq(USER_ID), any()))
        .thenReturn(List.of(SubscriptionInvoicePeriod.builder().invoiceId("invoice_id").build()));
    when(invoiceRepository.findById("invoice_id")).thenReturn(invoice);
    when(invoicingService.issueCreditNoteInvoice(any(), eq(birdia), eq(customer)))
        .thenReturn(Invoice.builder().id("credit_note_id").ref("AV-1").build());

    subject.terminate(TERMINATION_ID);

    verify(userSubscriptionProductService).endActiveSubscriptionProducts(USER_ID, END);
    var draft = ArgumentCaptor.forClass(SubscriptionUpgradeInvoicingService.CreditNoteDraft.class);
    verify(invoicingService).issueCreditNoteInvoice(draft.capture(), eq(birdia), eq(customer));
    assertEquals(1, draft.getValue().lines().products().size());
    assertEquals(
        "Avoir Abonnement du 01/12/2026 au 31/12/2026",
        draft.getValue().lines().products().getFirst().getDescription());
    assertEquals(
        BigInteger.valueOf(-4900),
        draft.getValue().lines().products().getFirst().getUnitPrice().getNumerator());
    var saved = ArgumentCaptor.forClass(TempSubscriptionTermination.class);
    verify(repository, times(2)).save(saved.capture());
    assertEquals(CREDIT_NOTE_ISSUED, saved.getAllValues().getLast().getStatus());
    assertEquals("credit_note_id", saved.getAllValues().getLast().getCreditNoteInvoiceId());
  }

  @Test
  void terminate_credits_every_month_after_current_period_end_with_exact_dates() {
    var termination = termination("2026-11-30", 5880);
    when(repository.findById(TERMINATION_ID)).thenReturn(Optional.of(termination));
    when(livePeriodFinder.findLive(eq(USER_ID), any()))
        .thenReturn(List.of(SubscriptionInvoicePeriod.builder().invoiceId("invoice_id").build()));
    when(invoiceRepository.findById("invoice_id"))
        .thenReturn(invoiceWithMonths("2026-11-01", "2026-12-01", "2027-01-01", "2027-02-01"));
    when(invoicingService.issueCreditNoteInvoice(any(), eq(birdia), eq(customer)))
        .thenReturn(Invoice.builder().id("credit_note_id").ref("AV-1").build());

    subject.terminate(TERMINATION_ID);

    var captor = ArgumentCaptor.forClass(SubscriptionUpgradeInvoicingService.CreditNoteDraft.class);
    verify(invoicingService).issueCreditNoteInvoice(captor.capture(), eq(birdia), eq(customer));
    var draft = captor.getValue();
    assertEquals(
        "Avoir sur l'abonnement pour la période du 01/12/2026 au 28/02/2027 (facture FA-1)",
        draft.title());
    assertEquals(LocalDate.parse("2026-12-01"), draft.date());
    assertFalse(draft.yearlyBilled());
    assertEquals(
        List.of(
            "Avoir Abonnement du 01/12/2026 au 31/12/2026",
            "Avoir Abonnement du 01/01/2027 au 31/01/2027",
            "Avoir Abonnement du 01/02/2027 au 28/02/2027"),
        draft.lines().products().stream().map(InvoiceProduct::getDescription).toList());
    draft
        .lines()
        .products()
        .forEach(
            line -> {
              assertEquals(BigInteger.valueOf(-4900), line.getUnitPrice().getNumerator());
              assertEquals(draft.invoiceIdentifier(), line.getIdInvoice());
            });
  }

  @Test
  void terminate_skips_credit_note_when_nothing_is_billed_after_current_period_end() {
    var termination = termination("2026-11-30", 5880);
    when(repository.findById(TERMINATION_ID)).thenReturn(Optional.of(termination));
    when(livePeriodFinder.findLive(eq(USER_ID), any()))
        .thenReturn(List.of(SubscriptionInvoicePeriod.builder().invoiceId("invoice_id").build()));
    when(invoiceRepository.findById("invoice_id")).thenReturn(invoiceWithMonths("2026-11-01"));

    subject.terminate(TERMINATION_ID);

    verify(invoicingService, never()).issueCreditNoteInvoice(any(), any(), any());
    var saved = ArgumentCaptor.forClass(TempSubscriptionTermination.class);
    verify(repository, times(2)).save(saved.capture());
    assertEquals(SKIPPED, saved.getAllValues().getLast().getStatus());
  }

  @Test
  void terminate_marks_failed_and_issues_nothing_when_amount_differs() {
    var termination = termination("2026-11-30", 1234);
    when(repository.findById(TERMINATION_ID)).thenReturn(Optional.of(termination));
    when(livePeriodFinder.findLive(eq(USER_ID), any()))
        .thenReturn(List.of(SubscriptionInvoicePeriod.builder().invoiceId("invoice_id").build()));
    when(invoiceRepository.findById("invoice_id"))
        .thenReturn(invoiceWithMonths("2026-11-01", "2026-12-01"));

    subject.terminate(TERMINATION_ID);

    verify(invoicingService, never()).issueCreditNoteInvoice(any(), any(), any());
    var saved = ArgumentCaptor.forClass(TempSubscriptionTermination.class);
    verify(repository, times(2)).save(saved.capture());
    assertEquals(FAILED, saved.getAllValues().getLast().getStatus());
    assertTrue(saved.getAllValues().getLast().getErrorMessage().contains("does not match"));
  }

  @Test
  void terminate_ignores_already_credited_termination() {
    var termination =
        termination("2026-11-30", 5880).toBuilder().status(CREDIT_NOTE_ISSUED).build();
    when(repository.findById(TERMINATION_ID)).thenReturn(Optional.of(termination));

    subject.terminate(TERMINATION_ID);

    verify(userSubscriptionProductService, never()).endActiveSubscriptionProducts(any(), any());
    verify(repository, never()).save(any());
  }

  private TempSubscriptionTermination termination(String currentPeriodEnd, long amount) {
    return TempSubscriptionTermination.builder()
        .id(TERMINATION_ID)
        .userSubscriptionE2Id(E2_ID)
        .subscriptionEndDatetime(END)
        .currentPeriodEndDate(LocalDate.parse(currentPeriodEnd))
        .amountInCentsWithVat(amount)
        .status(PENDING)
        .build();
  }

  private Invoice invoiceWithMonths(String... monthStarts) {
    var products =
        java.util.Arrays.stream(monthStarts)
            .map(
                start ->
                    product(
                        start,
                        LocalDate.parse(start)
                            .withDayOfMonth(LocalDate.parse(start).lengthOfMonth())
                            .toString()))
            .toList();
    return Invoice.builder()
        .id("invoice_id")
        .ref("FA-1")
        .products(products)
        .discount(InvoiceDiscount.builder().percentValue(new Fraction(BigInteger.ZERO)).build())
        .build();
  }

  private BilledLine billedLine(String start, String end) {
    return new BilledLine(
        Invoice.builder().id("invoice_id").ref("FA-1").build(),
        product(start, end),
        LocalDate.parse(start),
        LocalDate.parse(end));
  }

  private InvoiceProduct product(String start, String end) {
    return InvoiceProduct.builder()
        .description(
            "Abonnement du "
                + customDateFormatter.formatFrenchDate(LocalDate.parse(start))
                + " au "
                + customDateFormatter.formatFrenchDate(LocalDate.parse(end)))
        .quantity(1)
        .unitPrice(new Fraction(BigInteger.valueOf(4900)))
        .vatPercent(new Fraction(BigInteger.valueOf(20)))
        .build();
  }
}
