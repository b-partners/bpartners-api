package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.InvoiceSummaryContent;
import app.bpartners.api.endpoint.rest.model.InvoicesSummary;
import org.springframework.stereotype.Component;

@Component
public class InvoicesSummaryRestMapper {
  public InvoicesSummary toRest(app.bpartners.api.model.InvoicesSummary domain) {
    return new InvoicesSummary()
        .paid(toRest(domain.getPaid()))
        .unpaid(toRest(domain.getUnpaid()))
        .proposal(toRest(domain.getProposal()));
  }

  private InvoiceSummaryContent toRest(
      app.bpartners.api.model.InvoicesSummary.InvoiceSummaryContent domain) {
    return new InvoiceSummaryContent()
        .amount(domain.getAmount().getCents())
        .count(domain.getCount());
  }
}
