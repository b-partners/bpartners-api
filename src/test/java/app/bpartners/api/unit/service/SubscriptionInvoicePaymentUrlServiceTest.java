package app.bpartners.api.unit.service;

import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.User;
import app.bpartners.api.service.subscription.StripeInvoiceService;
import app.bpartners.api.service.subscription.SubscriptionInvoicePaymentUrlService;
import app.bpartners.api.service.user.UserService;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SubscriptionInvoicePaymentUrlServiceTest {
  private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
  private static final String USER_ID = "user_id";
  private static final String STRIPE_CUSTOMER_ID = "stripe_customer_id";
  private static final String INVOICE_ID = "invoice_id";
  private static final String HOSTED_INVOICE_URL = "https://stripe/hosted/invoice";
  private static final long TOTAL_IN_CENTS = 1200L;

  UserService userServiceMock = mock();
  StripeInvoiceService stripeInvoiceServiceMock = mock();

  SubscriptionInvoicePaymentUrlService subject =
      new SubscriptionInvoicePaymentUrlService(userServiceMock, stripeInvoiceServiceMock);

  @Test
  void no_stripe_lookup_when_no_unpaid_invoice() {
    var paidInvoice = invoice(INVOICE_ID, PAID, TOTAL_IN_CENTS, YearMonth.of(2026, 8));

    var actual = subject.getPaymentUrlByInvoiceId(USER_ID, List.of(paidInvoice));

    assertTrue(actual.isEmpty());
    verify(userServiceMock, never()).getUserById(any());
    verify(stripeInvoiceServiceMock, never()).getUnpaidStripeInvoices(any());
  }

  @Test
  void no_payment_url_when_no_stripe_customer() {
    var unpaidInvoice = invoice(INVOICE_ID, CONFIRMED, TOTAL_IN_CENTS, YearMonth.of(2026, 8));
    when(userServiceMock.getUserById(USER_ID))
        .thenReturn(User.builder().id(USER_ID).userSubscriptionId(null).build());

    var actual = subject.getPaymentUrlByInvoiceId(USER_ID, List.of(unpaidInvoice));

    assertTrue(actual.isEmpty());
    verify(stripeInvoiceServiceMock, never()).getUnpaidStripeInvoices(any());
  }

  @Test
  void matches_stripe_hosted_url_by_amount() {
    var unpaidInvoice = invoice(INVOICE_ID, CONFIRMED, TOTAL_IN_CENTS, YearMonth.of(2026, 8));
    when(userServiceMock.getUserById(USER_ID)).thenReturn(userWithStripeCustomer());
    when(stripeInvoiceServiceMock.getUnpaidStripeInvoices(STRIPE_CUSTOMER_ID))
        .thenReturn(
            List.of(
                stripeInvoice(9999L, "https://stripe/other", YearMonth.of(2026, 8)),
                stripeInvoice(TOTAL_IN_CENTS, HOSTED_INVOICE_URL, YearMonth.of(2026, 8))));

    var actual = subject.getPaymentUrlByInvoiceId(USER_ID, List.of(unpaidInvoice));

    assertEquals(Map.of(INVOICE_ID, HOSTED_INVOICE_URL), actual);
  }

  @Test
  void falls_back_to_single_unpaid_stripe_invoice_when_amount_mismatch() {
    var unpaidInvoice = invoice(INVOICE_ID, CONFIRMED, 42L, YearMonth.of(2026, 8));
    when(userServiceMock.getUserById(USER_ID)).thenReturn(userWithStripeCustomer());
    when(stripeInvoiceServiceMock.getUnpaidStripeInvoices(STRIPE_CUSTOMER_ID))
        .thenReturn(
            List.of(stripeInvoice(TOTAL_IN_CENTS, HOSTED_INVOICE_URL, YearMonth.of(2026, 8))));

    var actual = subject.getPaymentUrlByInvoiceId(USER_ID, List.of(unpaidInvoice));

    assertEquals(Map.of(INVOICE_ID, HOSTED_INVOICE_URL), actual);
  }

  @Test
  void no_entry_when_amount_mismatch_and_several_unpaid_stripe_invoices() {
    var unpaidInvoice = invoice(INVOICE_ID, CONFIRMED, 42L, YearMonth.of(2026, 8));
    when(userServiceMock.getUserById(USER_ID)).thenReturn(userWithStripeCustomer());
    when(stripeInvoiceServiceMock.getUnpaidStripeInvoices(STRIPE_CUSTOMER_ID))
        .thenReturn(
            List.of(
                stripeInvoice(TOTAL_IN_CENTS, HOSTED_INVOICE_URL, YearMonth.of(2026, 8)),
                stripeInvoice(9999L, "https://stripe/other", YearMonth.of(2026, 8))));

    var actual = subject.getPaymentUrlByInvoiceId(USER_ID, List.of(unpaidInvoice));

    assertTrue(actual.isEmpty());
  }

  @Test
  void same_amount_stripe_invoices_are_disambiguated_by_period() {
    var julyInvoice = invoice("invoice_july", CONFIRMED, TOTAL_IN_CENTS, YearMonth.of(2026, 7));
    var augustInvoice = invoice("invoice_august", CONFIRMED, TOTAL_IN_CENTS, YearMonth.of(2026, 8));
    when(userServiceMock.getUserById(USER_ID)).thenReturn(userWithStripeCustomer());
    when(stripeInvoiceServiceMock.getUnpaidStripeInvoices(STRIPE_CUSTOMER_ID))
        .thenReturn(
            List.of(
                stripeInvoice(TOTAL_IN_CENTS, "https://stripe/august", YearMonth.of(2026, 8)),
                stripeInvoice(TOTAL_IN_CENTS, "https://stripe/july", YearMonth.of(2026, 7))));

    var actual = subject.getPaymentUrlByInvoiceId(USER_ID, List.of(augustInvoice, julyInvoice));

    assertEquals(
        Map.of(
            "invoice_july", "https://stripe/july",
            "invoice_august", "https://stripe/august"),
        actual);
  }

  @Test
  void a_stripe_invoice_is_not_reused_across_internal_invoices() {
    var julyInvoice = invoice("invoice_july", CONFIRMED, TOTAL_IN_CENTS, YearMonth.of(2026, 7));
    var augustInvoice = invoice("invoice_august", CONFIRMED, TOTAL_IN_CENTS, YearMonth.of(2026, 8));
    when(userServiceMock.getUserById(USER_ID)).thenReturn(userWithStripeCustomer());
    when(stripeInvoiceServiceMock.getUnpaidStripeInvoices(STRIPE_CUSTOMER_ID))
        .thenReturn(
            List.of(stripeInvoice(TOTAL_IN_CENTS, HOSTED_INVOICE_URL, YearMonth.of(2026, 8))));

    var actual = subject.getPaymentUrlByInvoiceId(USER_ID, List.of(augustInvoice, julyInvoice));

    assertEquals(Map.of("invoice_august", HOSTED_INVOICE_URL), actual);
  }

  @Test
  void no_single_fallback_when_several_internal_unpaid_invoices() {
    var julyInvoice = invoice("invoice_july", CONFIRMED, 42L, YearMonth.of(2026, 7));
    var augustInvoice = invoice("invoice_august", CONFIRMED, 42L, YearMonth.of(2026, 8));
    when(userServiceMock.getUserById(USER_ID)).thenReturn(userWithStripeCustomer());
    when(stripeInvoiceServiceMock.getUnpaidStripeInvoices(STRIPE_CUSTOMER_ID))
        .thenReturn(
            List.of(stripeInvoice(TOTAL_IN_CENTS, HOSTED_INVOICE_URL, YearMonth.of(2026, 8))));

    var actual = subject.getPaymentUrlByInvoiceId(USER_ID, List.of(augustInvoice, julyInvoice));

    assertTrue(actual.isEmpty());
  }

  private static User userWithStripeCustomer() {
    return User.builder().id(USER_ID).userSubscriptionId(STRIPE_CUSTOMER_ID).build();
  }

  private static Invoice invoice(
      String id,
      app.bpartners.api.endpoint.rest.model.InvoiceStatus status,
      long totalInCents,
      YearMonth period) {
    return Invoice.builder()
        .id(id)
        .status(status)
        .sendingDate(period.atEndOfMonth())
        .totalPriceWithVat(new Fraction(BigInteger.valueOf(totalInCents)))
        .build();
  }

  private static com.stripe.model.Invoice stripeInvoice(
      long total, String hostedInvoiceUrl, YearMonth period) {
    var stripeInvoice = new com.stripe.model.Invoice();
    stripeInvoice.setTotal(total);
    stripeInvoice.setHostedInvoiceUrl(hostedInvoiceUrl);
    stripeInvoice.setPeriodEnd(epochSecondsOf(period));
    return stripeInvoice;
  }

  private static long epochSecondsOf(YearMonth period) {
    LocalDate endOfMonth = period.atEndOfMonth();
    return endOfMonth.atStartOfDay(PARIS).toInstant().getEpochSecond();
  }
}
