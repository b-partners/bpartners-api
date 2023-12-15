package app.bpartners.api.service.event;

import app.bpartners.api.endpoint.event.gen.RefreshTransactionsSummariesTriggered;
import app.bpartners.api.service.TransactionService;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class RefreshTransactionsSummariesTriggeredService
    implements Consumer<RefreshTransactionsSummariesTriggered> {
  private final TransactionService service;

  @Override
  public void accept(RefreshTransactionsSummariesTriggered refreshTransactionsSummariesTriggered) {
    service.refreshTransactionsSummaries();
  }
}
