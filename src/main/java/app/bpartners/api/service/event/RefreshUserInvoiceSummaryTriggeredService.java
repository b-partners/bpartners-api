package app.bpartners.api.service.event;

import app.bpartners.api.endpoint.event.gen.RefreshUserInvoiceSummaryTriggered;
import app.bpartners.api.service.InvoiceSummaryService;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class RefreshUserInvoiceSummaryTriggeredService
    implements Consumer<RefreshUserInvoiceSummaryTriggered> {
  private final InvoiceSummaryService invoiceSummaryService;

  @Override
  public void accept(RefreshUserInvoiceSummaryTriggered RefreshUserInvoiceSummaryTriggered) {
    invoiceSummaryService.updateInvoiceSummary(RefreshUserInvoiceSummaryTriggered.getUserId());
  }
}
