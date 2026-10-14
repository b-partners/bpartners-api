package app.bpartners.api.service.subscription;

import static app.bpartners.api.model.subscription.BillingInterval.MONTHLY;
import static app.bpartners.api.model.subscription.BillingInterval.YEARLY;
import static app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.INVOICED;
import static app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.PAID;
import static app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.PENDING;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.model.Invoice.PaymentTypeEnum;
import app.bpartners.api.model.Customer;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.subscription.SubscriptionInvoicePeriod;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.model.subscription.UserSubscriptionUpgrade;
import app.bpartners.api.payment.UserSubscriptionConf;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.jpa.SubscriptionInvoicePeriodRepository;
import app.bpartners.api.repository.jpa.SubscriptionProductRepository;
import app.bpartners.api.repository.jpa.UserSubscriptionUpgradeJpaRepository;
import app.bpartners.api.service.customer.SubscriptionCustomerResolver;
import app.bpartners.api.service.invoice.InvoiceService;
import app.bpartners.api.service.utils.CustomDateFormatter;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SubscriptionUpgradeInvoicingServiceTest {
  private static final String UPGRADE_ID = "upgrade_id";
  private static final String USER_TO_CREDIT_ID = "birdia_id";
  private static final String USER_ID = "subscriber_id";

  UserSubscriptionUpgradeJpaRepository upgradeRepository = mock();
  SubscriptionProductRepository subscriptionProductRepository = mock();
  SubscriptionInvoicePeriodRepository subscriptionInvoicePeriodRepository = mock();
  UserRepository userRepository = mock();
  SubscriptionCustomerResolver subscriptionCustomerResolver = mock();
  InvoiceService invoiceService = mock();
  InvoiceRepository invoiceRepository = mock();
  SubscriptionInvoiceMailer subscriptionInvoiceMailer = mock();
  ChargedFirstMonthResolver chargedFirstMonthResolver = mock();
  CustomDateFormatter customDateFormatter = new CustomDateFormatter();

  SubscriptionUpgradeInvoicingService subject =
      new SubscriptionUpgradeInvoicingService(
          upgradeRepository,
          subscriptionProductRepository,
          subscriptionInvoicePeriodRepository,
          userRepository,
          new UserSubscriptionConf(USER_TO_CREDIT_ID),
          subscriptionCustomerResolver,
          invoiceService,
          invoiceRepository,
          customDateFormatter,
          new SubscriptionUpgradeInvoiceLines(customDateFormatter),
          subscriptionInvoiceMailer,
          chargedFirstMonthResolver);

  SubscriptionProduct essential = product("essential_id", "Essentiel", 5_000L);
  SubscriptionProduct pro = product("pro_id", "Pro", 10_000L);

  @BeforeEach
  void setUp() {
    when(subscriptionProductRepository.findById("essential_id")).thenReturn(Optional.of(essential));
    when(subscriptionProductRepository.findById("pro_id")).thenReturn(Optional.of(pro));
    when(userRepository.getById(USER_TO_CREDIT_ID))
        .thenReturn(User.builder().id(USER_TO_CREDIT_ID).build());
    when(userRepository.getById(USER_ID)).thenReturn(User.builder().id(USER_ID).build());
    when(subscriptionCustomerResolver.apply(any(), any())).thenReturn(Customer.builder().build());
    when(invoiceService.crupdateSubscriptionInvoice(any(Invoice.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(invoiceRepository.findByIdUserAndRef(anyString(), anyString())).thenReturn(List.of());
    when(subscriptionInvoicePeriodRepository.findByUserId(USER_ID)).thenReturn(List.of());
    when(upgradeRepository.save(any(UserSubscriptionUpgrade.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void issues_a_negative_credit_note_then_the_new_invoice_and_flags_the_upgrade_invoiced() {
    when(upgradeRepository.findById(UPGRADE_ID)).thenReturn(Optional.of(upgrade(PAID)));

    var actual = subject.invoice(UPGRADE_ID).orElseThrow();

    assertEquals(INVOICED, actual.getStatus());
    assertNotNull(actual.getCreditNoteInvoiceId());
    assertNotNull(actual.getNewInvoiceId());
    var invoices = ArgumentCaptor.forClass(Invoice.class);
    verify(invoiceService, times(2)).crupdateSubscriptionInvoice(invoices.capture());
    var creditNote = invoices.getAllValues().get(0);
    var newInvoice = invoices.getAllValues().get(1);
    assertTrue(creditNote.getTotalPriceWithVat().getApproximatedValue() < 0);
    assertTrue(creditNote.getTitle().startsWith("Avoir sur l'abonnement Essentiel"));
    assertTrue(newInvoice.getTotalPriceWithVat().getApproximatedValue() > 0);
    assertEquals(creditNote.getId(), actual.getCreditNoteInvoiceId());
    assertEquals(newInvoice.getId(), actual.getNewInvoiceId());
    verify(subscriptionInvoicePeriodRepository).save(any());
  }

  @Test
  void credit_note_first_line_prorates_the_amount_actually_charged_on_the_first_month() {
    when(upgradeRepository.findById(UPGRADE_ID)).thenReturn(Optional.of(upgrade(PAID)));
    when(chargedFirstMonthResolver.resolve(USER_ID, LocalDate.of(2026, 10, 12)))
        .thenReturn(Optional.of(chargedFirstMonth(4_100L)));

    subject.invoice(UPGRADE_ID);

    var creditNote = captureIssuedInvoices().get(0);
    var first = creditNote.getProducts().get(0);
    assertEquals(BigInteger.valueOf(-82_000L), first.getUnitPrice().getNumerator());
    assertEquals(BigInteger.valueOf(27L), first.getUnitPrice().getDenominator());
    assertEquals(
        BigInteger.valueOf(-5_000L), creditNote.getProducts().get(1).getUnitPrice().getNumerator());
    assertTrue(first.getDescription().startsWith("Avoir Abonnement Essentiel du 12/10/2026"));
  }

  @Test
  void credit_note_first_line_matches_the_credit_charged_in_the_quote() {
    when(upgradeRepository.findById(UPGRADE_ID)).thenReturn(Optional.of(upgrade(PAID)));
    when(chargedFirstMonthResolver.resolve(USER_ID, LocalDate.of(2026, 10, 12)))
        .thenReturn(Optional.of(chargedFirstMonth(4_100L)));

    subject.invoice(UPGRADE_ID);

    var first = captureIssuedInvoices().get(0).getProducts().get(0);
    var firstLineWithVatInCents = Math.abs(first.getUnitPrice().getApproximatedValue()) * 1.2;
    assertEquals(
        new SubscriptionUpgradeQuoteCalculator()
            .compute(
                essential,
                MONTHLY,
                pro,
                MONTHLY,
                LocalDate.of(2026, 10, 12),
                LocalDate.of(2027, 9, 30),
                Optional.of(chargedFirstMonth(4_100L)))
            .creditedInCentsWithVat(),
        Math.round(firstLineWithVatInCents));
  }

  @Test
  void credit_note_first_line_stays_day_based_without_a_charged_first_month() {
    when(upgradeRepository.findById(UPGRADE_ID)).thenReturn(Optional.of(upgrade(PAID)));
    when(chargedFirstMonthResolver.resolve(USER_ID, LocalDate.of(2026, 10, 12)))
        .thenReturn(Optional.empty());

    subject.invoice(UPGRADE_ID);

    var first = captureIssuedInvoices().get(0).getProducts().get(0);
    assertEquals(BigInteger.valueOf(-100_000L), first.getUnitPrice().getNumerator());
    assertEquals(BigInteger.valueOf(31L), first.getUnitPrice().getDenominator());
  }

  @Test
  void new_invoice_lines_do_not_depend_on_the_charged_first_month() {
    when(upgradeRepository.findById(UPGRADE_ID)).thenReturn(Optional.of(upgrade(PAID)));
    when(chargedFirstMonthResolver.resolve(USER_ID, LocalDate.of(2026, 10, 12)))
        .thenReturn(Optional.of(chargedFirstMonth(4_100L)));

    subject.invoice(UPGRADE_ID);

    var first = captureIssuedInvoices().get(1).getProducts().get(0);
    assertEquals(BigInteger.valueOf(200_000L), first.getUnitPrice().getNumerator());
    assertEquals(BigInteger.valueOf(31L), first.getUnitPrice().getDenominator());
  }

  @Test
  void credit_note_of_a_yearly_subscription_does_not_use_the_charged_first_month() {
    when(upgradeRepository.findById(UPGRADE_ID))
        .thenReturn(
            Optional.of(
                upgrade(PAID).toBuilder()
                    .fromBillingInterval(YEARLY)
                    .toBillingInterval(YEARLY)
                    .oldCoverageEndDatetime(Instant.parse("2027-03-30T22:00:00Z"))
                    .build()));

    subject.invoice(UPGRADE_ID);

    verifyNoInteractions(chargedFirstMonthResolver);
  }

  @Test
  void does_not_resolve_the_charged_month_when_the_credit_note_already_exists() {
    when(upgradeRepository.findById(UPGRADE_ID))
        .thenReturn(
            Optional.of(upgrade(PAID).toBuilder().creditNoteInvoiceId("credit_note_id").build()));

    subject.invoice(UPGRADE_ID);

    verifyNoInteractions(chargedFirstMonthResolver);
  }

  private List<Invoice> captureIssuedInvoices() {
    var invoices = ArgumentCaptor.forClass(Invoice.class);
    verify(invoiceService, times(2)).crupdateSubscriptionInvoice(invoices.capture());
    return invoices.getAllValues();
  }

  private static ChargedFirstMonth chargedFirstMonth(long chargedInCentsWithoutVat) {
    return new ChargedFirstMonth(
        LocalDate.of(2026, 10, 5),
        LocalDate.of(2026, 10, 31),
        new Fraction(BigInteger.valueOf(chargedInCentsWithoutVat)),
        new Fraction(BigInteger.valueOf(2_000L)));
  }

  @Test
  void issues_only_the_missing_new_invoice_when_the_credit_note_already_exists() {
    when(upgradeRepository.findById(UPGRADE_ID))
        .thenReturn(
            Optional.of(upgrade(PAID).toBuilder().creditNoteInvoiceId("credit_note_id").build()));

    var actual = subject.invoice(UPGRADE_ID).orElseThrow();

    verify(invoiceService, times(1)).crupdateSubscriptionInvoice(any(Invoice.class));
    assertEquals("credit_note_id", actual.getCreditNoteInvoiceId());
    assertEquals(INVOICED, actual.getStatus());
  }

  @Test
  void skips_an_already_invoiced_upgrade() {
    when(upgradeRepository.findById(UPGRADE_ID))
        .thenReturn(
            Optional.of(
                upgrade(INVOICED).toBuilder()
                    .creditNoteInvoiceId("credit_note_id")
                    .newInvoiceId("new_invoice_id")
                    .build()));

    assertTrue(subject.invoice(UPGRADE_ID).isEmpty());

    verifyNoInteractions(invoiceService);
  }

  @Test
  void skips_a_pending_upgrade() {
    when(upgradeRepository.findById(UPGRADE_ID)).thenReturn(Optional.of(upgrade(PENDING)));

    assertTrue(subject.invoice(UPGRADE_ID).isEmpty());

    verify(invoiceService, never()).crupdateSubscriptionInvoice(any());
  }

  @Test
  void fails_when_the_upgrade_does_not_exist() {
    when(upgradeRepository.findById(UPGRADE_ID)).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> subject.invoice(UPGRADE_ID));
  }

  @Test
  void fails_when_a_plan_of_the_upgrade_does_not_exist() {
    when(upgradeRepository.findById(UPGRADE_ID)).thenReturn(Optional.of(upgrade(PAID)));
    when(subscriptionProductRepository.findById("essential_id")).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> subject.invoice(UPGRADE_ID));
  }

  @Test
  void issues_settled_invoices_for_a_yearly_upgrade_covering_twelve_months() {
    when(upgradeRepository.findById(UPGRADE_ID))
        .thenReturn(
            Optional.of(
                upgrade(PAID).toBuilder()
                    .fromBillingInterval(YEARLY)
                    .toBillingInterval(YEARLY)
                    .build()));

    subject.invoice(UPGRADE_ID).orElseThrow();

    var invoices = ArgumentCaptor.forClass(Invoice.class);
    verify(invoiceService, times(2)).crupdateSubscriptionInvoice(invoices.capture());
    var newInvoice = invoices.getAllValues().get(1);
    assertEquals(YEARLY, newInvoice.getSubscriptionBillingInterval());
    assertEquals(PaymentTypeEnum.CASH, newInvoice.getPaymentType());
    assertEquals(13, newInvoice.getProducts().size());
    var period = ArgumentCaptor.forClass(SubscriptionInvoicePeriod.class);
    verify(subscriptionInvoicePeriodRepository).save(period.capture());
    assertEquals(Instant.parse("2027-10-10T22:00:00Z"), period.getValue().getPeriodEndDatetime());
  }

  @Test
  void issues_only_the_credit_note_when_the_new_invoice_already_exists() {
    when(upgradeRepository.findById(UPGRADE_ID))
        .thenReturn(Optional.of(upgrade(PAID).toBuilder().newInvoiceId("new_invoice_id").build()));

    var actual = subject.invoice(UPGRADE_ID).orElseThrow();

    verify(invoiceService, times(1)).crupdateSubscriptionInvoice(any(Invoice.class));
    assertEquals("new_invoice_id", actual.getNewInvoiceId());
    assertNotNull(actual.getCreditNoteInvoiceId());
  }

  @Test
  void mentions_the_original_invoice_in_the_credit_note_title() {
    when(upgradeRepository.findById(UPGRADE_ID)).thenReturn(Optional.of(upgrade(PAID)));
    when(subscriptionInvoicePeriodRepository.findByUserId(USER_ID))
        .thenReturn(
            List.of(
                period("no_end_invoice", null),
                period("future_invoice", "2028-01-01"),
                period("early_invoice", "2027-03-31"),
                period("latest_invoice", "2027-09-30")));
    when(invoiceRepository.findById("latest_invoice"))
        .thenReturn(Invoice.builder().id("latest_invoice").ref("REF-LATEST").build());

    subject.invoice(UPGRADE_ID).orElseThrow();

    var invoices = ArgumentCaptor.forClass(Invoice.class);
    verify(invoiceService, times(2)).crupdateSubscriptionInvoice(invoices.capture());
    assertTrue(invoices.getAllValues().get(0).getTitle().endsWith("(facture REF-LATEST)"));
  }

  @Test
  void leaves_the_original_invoice_out_of_the_title_when_it_cannot_be_found() {
    when(upgradeRepository.findById(UPGRADE_ID)).thenReturn(Optional.of(upgrade(PAID)));
    when(subscriptionInvoicePeriodRepository.findByUserId(USER_ID))
        .thenReturn(List.of(period("missing_invoice", "2027-09-30")));
    when(invoiceRepository.findById("missing_invoice")).thenReturn(null);

    subject.invoice(UPGRADE_ID).orElseThrow();

    var invoices = ArgumentCaptor.forClass(Invoice.class);
    verify(invoiceService, times(2)).crupdateSubscriptionInvoice(invoices.capture());
    assertFalse(invoices.getAllValues().get(0).getTitle().contains("(facture"));
  }

  @Test
  void moves_on_to_the_next_second_when_the_invoice_reference_is_already_taken() {
    when(upgradeRepository.findById(UPGRADE_ID)).thenReturn(Optional.of(upgrade(PAID)));
    when(invoiceRepository.findByIdUserAndRef(anyString(), anyString()))
        .thenReturn(List.of(Invoice.builder().build()), List.of());

    assertTrue(subject.invoice(UPGRADE_ID).isPresent());
  }

  @Test
  void fails_when_no_invoice_reference_is_available() {
    when(upgradeRepository.findById(UPGRADE_ID)).thenReturn(Optional.of(upgrade(PAID)));
    when(invoiceRepository.findByIdUserAndRef(anyString(), anyString()))
        .thenReturn(List.of(Invoice.builder().build()));

    assertThrows(ApiException.class, () -> subject.invoice(UPGRADE_ID));
  }

  @Test
  void keeps_the_upgrade_invoiced_when_the_mail_cannot_be_sent() {
    when(upgradeRepository.findById(UPGRADE_ID)).thenReturn(Optional.of(upgrade(PAID)));
    when(subscriptionInvoiceMailer.send(any(), any(), anyString(), any()))
        .thenThrow(new IllegalStateException("smtp down"));

    assertEquals(INVOICED, subject.invoice(UPGRADE_ID).orElseThrow().getStatus());
  }

  @Test
  void computes_the_last_full_month_end_of_a_commitment() {
    assertEquals(
        LocalDate.of(2027, 10, 31),
        SubscriptionUpgradeInvoicingService.lastFullMonthEnd(LocalDate.of(2027, 11, 1)));
    assertEquals(
        LocalDate.of(2027, 9, 30),
        SubscriptionUpgradeInvoicingService.lastFullMonthEnd(LocalDate.of(2027, 10, 15)));
  }

  private static SubscriptionInvoicePeriod period(String invoiceId, String endDate) {
    return SubscriptionInvoicePeriod.builder()
        .id("period_" + invoiceId)
        .userId(USER_ID)
        .invoiceId(invoiceId)
        .periodEndDatetime(
            endDate == null
                ? null
                : LocalDate.parse(endDate).atStartOfDay(ZoneId.of("Europe/Paris")).toInstant())
        .build();
  }

  private static UserSubscriptionUpgrade upgrade(
      app.bpartners.api.model.subscription.SubscriptionUpgradeStatus status) {
    return UserSubscriptionUpgrade.builder()
        .id(UPGRADE_ID)
        .userId(USER_ID)
        .fromSubscriptionProductId("essential_id")
        .toSubscriptionProductId("pro_id")
        .fromBillingInterval(MONTHLY)
        .toBillingInterval(MONTHLY)
        .upgradeDatetime(Instant.parse("2026-10-11T22:00:00Z"))
        .oldCoverageEndDatetime(Instant.parse("2027-09-29T22:00:00Z"))
        .newCommitmentEndDatetime(Instant.parse("2027-10-11T22:00:00Z"))
        .prorationAmountInCentsWithVat(3_871L)
        .status(status)
        .build();
  }

  private static SubscriptionProduct product(String id, String name, long priceInCentsWithoutVat) {
    return SubscriptionProduct.builder()
        .id(id)
        .name(name)
        .vatPercent(2_000L)
        .priceInCentsWithoutVat(priceInCentsWithoutVat)
        .annualPriceInCentsWithVat(priceInCentsWithoutVat * 12 * 12 / 10)
        .build();
  }
}
