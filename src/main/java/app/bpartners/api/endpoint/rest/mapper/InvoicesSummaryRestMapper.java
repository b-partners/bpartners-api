package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.InvoiceSummaryContent;
import app.bpartners.api.endpoint.rest.model.InvoicesSummary;
import app.bpartners.api.model.InvoiceSummary;
import org.springframework.stereotype.Component;

@Component
public class InvoicesSummaryRestMapper {

  public static final int DEFAULT_NOT_PROVIDED_VALUE = -1;

  public InvoicesSummary toRest(InvoiceSummary domain) {
    return new InvoicesSummary()
        .lastUpdateDatetime(domain.getUpdatedAt())
        .paid(toRest(domain.getPaid()))
        .unpaid(toRest(domain.getUnpaid()))
        .proposal(toRest(domain.getProposal()));
  }

  private InvoiceSummaryContent toRest(InvoiceSummary.InvoiceSummaryContent domain) {
    return new InvoiceSummaryContent()
        .amount(domain.getAmount().getCents())
        .count(DEFAULT_NOT_PROVIDED_VALUE);
  }
}
