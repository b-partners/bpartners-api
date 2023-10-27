package app.bpartners.api.service;

import app.bpartners.api.endpoint.rest.model.ArchiveStatus;
import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.model.BoundedPageSize;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.PageFromOne;
import java.util.List;
import javax.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static app.bpartners.api.model.BoundedPageSize.MAX_SIZE;
import static app.bpartners.api.model.PageFromOne.MIN_PAGE;

@Component
@AllArgsConstructor
@Slf4j
public class AlphanuisibleInvoiceComponent {
  public static final String ALPHANUISIBLE_USER_ID = "5e688f8a-91f8-42fc-a7c6-d979d4e455b8";
  private final InvoiceService invoiceService;

  @PostConstruct
  public void updateInvoicesBic() {
    List<Invoice> unpaidInvoices =
        invoiceService.getInvoices(
            ALPHANUISIBLE_USER_ID,
            new PageFromOne(MIN_PAGE),
            new BoundedPageSize(MAX_SIZE),
            List.of(InvoiceStatus.CONFIRMED, InvoiceStatus.PROPOSAL),
            ArchiveStatus.ENABLED,
            null,
            List.of());
    unpaidInvoices.forEach(invoice -> {
      invoiceService.crupdateInvoice(invoice);
      log.info("Invoice(id=" + invoice.getId()
          + ", reference=" + invoice.getRealReference() + ") was refreshed");
    });
  }
}
