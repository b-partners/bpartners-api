package app.bpartners.api.service.event;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.gen.RefreshInvoiceSummaryTriggered;
import app.bpartners.api.endpoint.event.gen.RefreshUserInvoiceSummaryTriggered;
import app.bpartners.api.endpoint.rest.model.EnableStatus;
import app.bpartners.api.service.UserService;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@AllArgsConstructor
@Service
public class RefreshInvoiceSummaryTriggeredService
    implements Consumer<RefreshInvoiceSummaryTriggered> {
  private final UserService userService;
  private final EventProducer eventProducer;

  @Override
  public void accept(RefreshInvoiceSummaryTriggered refreshInvoiceSummaryTriggered) {
    sendRefreshInvoiceSummaryByUserEvents();
  }

  private void sendRefreshInvoiceSummaryByUserEvents() {
    userService.findAll().stream()
        .filter(user -> user.getStatus() == EnableStatus.ENABLED)
        .forEach(user -> sendEvent(user.getId()));
  }

  private void sendEvent(String userId) {
    eventProducer.accept(
        List.of(RefreshUserInvoiceSummaryTriggered.builder().userId(userId).build()));
  }
}
