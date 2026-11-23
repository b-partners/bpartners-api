package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.SubscriptionInvoiceMonthSummary;
import app.bpartners.api.endpoint.rest.model.SubscriptionInvoiceSummaries;
import app.bpartners.api.endpoint.rest.model.SubscriptionInvoiceSummary;
import app.bpartners.api.endpoint.rest.model.SubscriptionInvoiceType;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.subscription.SubscriptionInvoiceYearSummary;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionInvoiceSummaryRestMapper {

  public SubscriptionInvoiceSummaries toRest(SubscriptionInvoiceYearSummary summary) {
    return new SubscriptionInvoiceSummaries()
        .actualYear(summary.year())
        .availableYears(summary.availableYears())
        .months(
            summary.invoicesByMonth().entrySet().stream()
                .map(
                    entry -> {
                      var invoices = entry.getValue().stream().map(this::toRest).toList();
                      return new SubscriptionInvoiceMonthSummary()
                          .month(entry.getKey().getMonthValue())
                          .invoiceCount(invoices.size())
                          .invoices(invoices);
                    })
                .toList());
  }

  private SubscriptionInvoiceSummary toRest(Invoice invoice) {
    return new SubscriptionInvoiceSummary()
        .id(invoice.getId())
        .type(
            invoice.isCreditNote()
                ? SubscriptionInvoiceType.CREDIT_NOTE
                : SubscriptionInvoiceType.INVOICE)
        .title(invoice.getTitle())
        .reference(invoice.getRealReference())
        .sendingDate(invoice.getSendingDate())
        .totalPriceWithoutVat(invoice.getTotalPriceWithoutVat().getCentsRoundUp())
        .totalVat(invoice.getTotalVat().getCentsRoundUp())
        .totalPriceWithVat(invoice.getTotalPriceWithVat().getCentsRoundUp())
        .paymentStatus(invoice.paymentStatus());
  }
}
