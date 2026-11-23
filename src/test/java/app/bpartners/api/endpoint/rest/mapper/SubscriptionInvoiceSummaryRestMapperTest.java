package app.bpartners.api.endpoint.rest.mapper;

import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;
import static app.bpartners.api.endpoint.rest.model.PaymentStatus.UNPAID;
import static app.bpartners.api.endpoint.rest.model.SubscriptionInvoiceType.CREDIT_NOTE;
import static app.bpartners.api.endpoint.rest.model.SubscriptionInvoiceType.INVOICE;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.endpoint.rest.model.PaymentStatus;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.subscription.SubscriptionInvoiceYearSummary;
import java.math.BigInteger;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.List;
import org.junit.jupiter.api.Test;

class SubscriptionInvoiceSummaryRestMapperTest {
  SubscriptionInvoiceSummaryRestMapper subject = new SubscriptionInvoiceSummaryRestMapper();

  private static Invoice invoice(String id, boolean creditNote, InvoiceStatus status) {
    return Invoice.builder()
        .id(id)
        .title("title_" + id)
        .sendingDate(LocalDate.of(2025, 3, 1))
        .status(status)
        .creditNote(creditNote)
        .totalPriceWithoutVat(new Fraction(BigInteger.valueOf(1000)))
        .totalVat(new Fraction(BigInteger.valueOf(200)))
        .totalPriceWithVat(new Fraction(BigInteger.valueOf(1200)))
        .build();
  }

  @Test
  void maps_months_counts_and_invoice_summaries() {
    var byMonth = new LinkedHashMap<YearMonth, List<Invoice>>();
    byMonth.put(YearMonth.of(2025, 2), List.of());
    byMonth.put(
        YearMonth.of(2025, 3),
        List.of(invoice("invoice_1", false, PAID), invoice("credit_note_1", true, CONFIRMED)));

    var actual =
        subject.toRest(new SubscriptionInvoiceYearSummary(2025, List.of(2024, 2025), byMonth));

    assertEquals(2025, actual.getActualYear());
    assertEquals(List.of(2024, 2025), actual.getAvailableYears());
    assertEquals(2, actual.getMonths().size());
    var february = actual.getMonths().get(0);
    assertEquals(2, february.getMonth());
    assertEquals(0, february.getInvoiceCount());
    var march = actual.getMonths().get(1);
    assertEquals(3, march.getMonth());
    assertEquals(2, march.getInvoiceCount());
    var first = march.getInvoices().get(0);
    assertEquals("invoice_1", first.getId());
    assertEquals(INVOICE, first.getType());
    assertEquals("title_invoice_1", first.getTitle());
    assertEquals(LocalDate.of(2025, 3, 1), first.getSendingDate());
    assertEquals(1000, first.getTotalPriceWithoutVat());
    assertEquals(200, first.getTotalVat());
    assertEquals(1200, first.getTotalPriceWithVat());
    assertEquals(PaymentStatus.PAID, first.getPaymentStatus());
    var second = march.getInvoices().get(1);
    assertEquals(CREDIT_NOTE, second.getType());
    assertEquals(UNPAID, second.getPaymentStatus());
  }
}
