package app.bpartners.api.service.subscription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.InvoiceProduct;
import app.bpartners.api.model.subscription.SubscriptionInvoicePeriod;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.service.utils.CustomDateFormatter;
import java.math.BigInteger;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class ChargedFirstMonthResolverTest {
  private static final String USER_ID = "subscriber_id";
  private static final ZoneId PARIS = ZoneId.of("Europe/Paris");
  private static final Instant UPGRADE_CREATION = Instant.parse("2026-10-14T09:47:00Z");

  LiveSubscriptionInvoicePeriodFinder livePeriodFinder = mock();
  InvoiceRepository invoiceRepository = mock();

  ChargedFirstMonthResolver subject =
      new ChargedFirstMonthResolver(livePeriodFinder, invoiceRepository, new CustomDateFormatter());

  @Test
  void resolves_the_amount_charged_on_the_partial_month_containing_the_upgrade_date() {
    givenPeriod("invoice_id", LocalDate.of(2026, 10, 5), LocalDate.of(2027, 9, 30));
    givenInvoice("invoice_id", 4_100L, "Abonnement Essentiel du 05/10/2026 au 31/10/2026");

    var actual =
        subject.resolve(USER_ID, LocalDate.of(2026, 10, 12), UPGRADE_CREATION).orElseThrow();

    assertEquals(LocalDate.of(2026, 10, 5), actual.start());
    assertEquals(LocalDate.of(2026, 10, 31), actual.end());
    assertEquals(BigInteger.valueOf(4_100L), actual.chargedWithoutVat().getNumerator());
  }

  @Test
  void resolves_from_an_invoice_read_back_without_its_billing_interval() {
    givenPeriod("invoice_id", LocalDate.of(2026, 10, 14), LocalDate.of(2027, 9, 30));
    givenInvoice("invoice_id", 5_632L, "Abonnement Pro du 14/10/2026 au 31/10/2026");

    var actual =
        subject.resolve(USER_ID, LocalDate.of(2026, 10, 14), UPGRADE_CREATION).orElseThrow();

    assertEquals(BigInteger.valueOf(5_632L), actual.chargedWithoutVat().getNumerator());
    assertEquals(18, actual.days());
  }

  @Test
  void resolves_nothing_when_the_upgrade_happens_after_the_first_month() {
    givenPeriod("invoice_id", LocalDate.of(2026, 10, 5), LocalDate.of(2027, 9, 30));
    givenInvoice("invoice_id", 4_100L, "Abonnement Essentiel du 05/10/2026 au 31/10/2026");

    assertTrue(subject.resolve(USER_ID, LocalDate.of(2026, 11, 12), UPGRADE_CREATION).isEmpty());
  }

  @Test
  void resolves_nothing_for_a_period_starting_on_the_first_of_the_month() {
    givenPeriod("invoice_id", LocalDate.of(2026, 10, 1), LocalDate.of(2027, 9, 30));

    assertTrue(subject.resolve(USER_ID, LocalDate.of(2026, 10, 12), UPGRADE_CREATION).isEmpty());
  }

  @Test
  void resolves_nothing_when_no_line_covers_the_first_month() {
    givenPeriod("invoice_id", LocalDate.of(2026, 10, 5), LocalDate.of(2027, 9, 30));
    givenInvoice("invoice_id", 4_100L, "Abonnement Essentiel du 01/11/2026 au 30/11/2026");

    assertTrue(subject.resolve(USER_ID, LocalDate.of(2026, 10, 12), UPGRADE_CREATION).isEmpty());
  }

  @Test
  void resolves_on_the_first_and_on_the_last_day_of_the_partial_month() {
    givenPeriod("invoice_id", LocalDate.of(2026, 10, 5), LocalDate.of(2027, 9, 30));
    givenInvoice("invoice_id", 4_100L, "Abonnement Essentiel du 05/10/2026 au 31/10/2026");

    assertTrue(subject.resolve(USER_ID, LocalDate.of(2026, 10, 5), UPGRADE_CREATION).isPresent());
    assertTrue(subject.resolve(USER_ID, LocalDate.of(2026, 10, 31), UPGRADE_CREATION).isPresent());
    assertTrue(subject.resolve(USER_ID, LocalDate.of(2026, 10, 4), UPGRADE_CREATION).isEmpty());
  }

  @Test
  void prefers_the_original_invoice_over_a_later_one_starting_on_the_upgrade_date() {
    when(livePeriodFinder.findLive(USER_ID, UPGRADE_CREATION))
        .thenReturn(
            List.of(
                periodOf("upgrade_invoice_id", LocalDate.of(2026, 10, 12)),
                periodOf("original_invoice_id", LocalDate.of(2026, 10, 5))));
    givenInvoice("original_invoice_id", 4_100L, "Abonnement Essentiel du 05/10/2026 au 31/10/2026");
    givenInvoice("upgrade_invoice_id", 6_000L, "Abonnement Pro du 12/10/2026 au 31/10/2026");

    var actual =
        subject.resolve(USER_ID, LocalDate.of(2026, 10, 12), UPGRADE_CREATION).orElseThrow();

    assertEquals(LocalDate.of(2026, 10, 5), actual.start());
    assertEquals(BigInteger.valueOf(4_100L), actual.chargedWithoutVat().getNumerator());
    verify(invoiceRepository, never()).findById("upgrade_invoice_id");
  }

  @Test
  void resolves_the_amount_of_a_previous_upgrade_invoice_starting_mid_month() {
    givenPeriod("upgrade_invoice_id", LocalDate.of(2026, 10, 12), LocalDate.of(2027, 9, 30));
    givenInvoice("upgrade_invoice_id", 6_000L, "Abonnement Pro du 12/10/2026 au 31/10/2026");

    var actual =
        subject.resolve(USER_ID, LocalDate.of(2026, 10, 20), UPGRADE_CREATION).orElseThrow();

    assertEquals(LocalDate.of(2026, 10, 12), actual.start());
    assertEquals(20, actual.days());
  }

  @Test
  void resolves_nothing_when_the_invoice_cannot_be_found() {
    givenPeriod("invoice_id", LocalDate.of(2026, 10, 5), LocalDate.of(2027, 9, 30));
    when(invoiceRepository.findById("invoice_id")).thenReturn(null);

    assertTrue(subject.resolve(USER_ID, LocalDate.of(2026, 10, 12), UPGRADE_CREATION).isEmpty());
  }

  @Test
  void resolves_nothing_when_the_matching_line_is_not_a_positive_charge() {
    givenPeriod("invoice_id", LocalDate.of(2026, 10, 5), LocalDate.of(2027, 9, 30));
    givenInvoice("invoice_id", -4_100L, "Avoir Abonnement Essentiel du 05/10/2026 au 31/10/2026");

    assertTrue(subject.resolve(USER_ID, LocalDate.of(2026, 10, 12), UPGRADE_CREATION).isEmpty());
  }

  @Test
  void resolves_nothing_when_the_user_has_no_invoiced_period() {
    when(livePeriodFinder.findLive(USER_ID, UPGRADE_CREATION)).thenReturn(List.of());

    assertTrue(subject.resolve(USER_ID, LocalDate.of(2026, 10, 12), UPGRADE_CREATION).isEmpty());
  }

  @Test
  void falls_back_on_no_vat_when_the_line_has_none() {
    givenPeriod("invoice_id", LocalDate.of(2026, 10, 5), LocalDate.of(2027, 9, 30));
    when(invoiceRepository.findById("invoice_id"))
        .thenReturn(
            Invoice.builder()
                .id("invoice_id")
                .subscriptionInvoice(true)
                .products(
                    List.of(
                        InvoiceProduct.builder()
                            .description("Abonnement Essentiel du 05/10/2026 au 31/10/2026")
                            .unitPrice(new Fraction(BigInteger.valueOf(4_100L)))
                            .build()))
                .build());

    var actual =
        subject.resolve(USER_ID, LocalDate.of(2026, 10, 12), UPGRADE_CREATION).orElseThrow();

    assertEquals(BigInteger.ZERO, actual.vatPercent().getNumerator());
  }

  private static SubscriptionInvoicePeriod periodOf(String invoiceId, LocalDate start) {
    return SubscriptionInvoicePeriod.builder()
        .id("period_" + invoiceId)
        .userId(USER_ID)
        .invoiceId(invoiceId)
        .periodStartDatetime(start.atStartOfDay(PARIS).toInstant())
        .periodEndDatetime(LocalDate.of(2027, 9, 30).atStartOfDay(PARIS).toInstant())
        .build();
  }

  private void givenPeriod(String invoiceId, LocalDate start, LocalDate end) {
    when(livePeriodFinder.findLive(USER_ID, UPGRADE_CREATION))
        .thenReturn(
            List.of(
                SubscriptionInvoicePeriod.builder()
                    .id("period_id")
                    .userId(USER_ID)
                    .invoiceId(invoiceId)
                    .periodStartDatetime(start.atStartOfDay(PARIS).toInstant())
                    .periodEndDatetime(end.atStartOfDay(PARIS).toInstant())
                    .build()));
  }

  private void givenInvoice(String invoiceId, long unitPriceInCents, String description) {
    when(invoiceRepository.findById(invoiceId))
        .thenReturn(
            Invoice.builder()
                .id(invoiceId)
                .subscriptionInvoice(true)
                .products(
                    List.of(
                        InvoiceProduct.builder()
                            .description(description)
                            .unitPrice(new Fraction(BigInteger.valueOf(unitPriceInCents)))
                            .vatPercent(new Fraction(BigInteger.valueOf(2_000L)))
                            .build()))
                .build());
  }
}
