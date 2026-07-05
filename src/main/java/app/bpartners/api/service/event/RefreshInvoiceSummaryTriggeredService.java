package app.bpartners.api.service.event;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.RefreshInvoiceSummaryTriggered;
import app.bpartners.api.endpoint.event.model.RefreshUserInvoiceSummaryTriggered;
import app.bpartners.api.endpoint.rest.model.EnableStatus;
import app.bpartners.api.model.User;
import app.bpartners.api.service.user.UserService;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
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
        .filter(
            user ->
                user.getStatus() == EnableStatus.ENABLED && user.getUserSubscriptionId() != null)
        .forEach(this::sendRefreshInvoiceSummaryEvent);
  }

  private void sendRefreshInvoiceSummaryEvent(User user) {
    try {
      eventProducer.accept(
          List.of(RefreshUserInvoiceSummaryTriggered.builder().userId(user.getId()).build()));
    } catch (RuntimeException e) {
      log.error("Unable to send RefreshUserInvoiceSummaryTriggered for user {}", user.getId(), e);
    }
  }
}
