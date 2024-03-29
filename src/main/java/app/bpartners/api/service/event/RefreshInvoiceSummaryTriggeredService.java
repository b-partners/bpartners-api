package app.bpartners.api.service.event;

import app.bpartners.api.endpoint.event.gen.RefreshInvoiceSummaryTriggered;
import app.bpartners.api.service.InvoiceSummaryService;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class RefreshInvoiceSummaryTriggeredService
    implements Consumer<RefreshInvoiceSummaryTriggered> {
  private final InvoiceSummaryService invoiceSummaryService;

  @Override
  public void accept(RefreshInvoiceSummaryTriggered refreshInvoiceSummaryTriggered) {
    invoiceSummaryService.updateInvoicesSummary();
  }
}
