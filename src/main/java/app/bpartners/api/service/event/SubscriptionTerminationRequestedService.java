package app.bpartners.api.service.event;

import app.bpartners.api.endpoint.event.model.SubscriptionTerminationRequested;
import app.bpartners.api.service.subscription.SubscriptionTerminationService;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriptionTerminationRequestedService
    implements Consumer<SubscriptionTerminationRequested> {
  private final SubscriptionTerminationService subscriptionTerminationService;

  @Override
  public void accept(SubscriptionTerminationRequested event) {
    subscriptionTerminationService.terminate(event.getTerminationId());
  }
}
