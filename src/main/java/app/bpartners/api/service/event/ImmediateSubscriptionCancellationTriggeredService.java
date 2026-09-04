package app.bpartners.api.service.event;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.ImmediateSubscriptionCancellationRequested;
import app.bpartners.api.endpoint.event.model.ImmediateSubscriptionCancellationTriggered;
import app.bpartners.api.model.User;
import app.bpartners.api.service.subscription.UpcomingUserDebitService;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImmediateSubscriptionCancellationTriggeredService
    implements Consumer<ImmediateSubscriptionCancellationTriggered> {
  private final UpcomingUserDebitService upcomingUserDebitService;
  private final EventProducer eventProducer;

  @Override
  public void accept(ImmediateSubscriptionCancellationTriggered event) {
    var billedUsers = upcomingUserDebitService.getUpcomingDebitedCustomers().billedUsers();
    log.info(
        "Immediate subscription cancellation triggered, fanning out for {} user(s) with an"
            + " active Stripe subscription",
        billedUsers.size());
    if (billedUsers.isEmpty()) {
      return;
    }
    eventProducer.accept(
        billedUsers.stream()
            .map(User::getId)
            .distinct()
            .map(
                userId ->
                    ImmediateSubscriptionCancellationRequested.builder().userId(userId).build())
            .toList());
  }
}
