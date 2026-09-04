package app.bpartners.api.service.event;

import app.bpartners.api.endpoint.event.model.ImmediateSubscriptionCancellationRequested;
import app.bpartners.api.service.subscription.SubscriptionService;
import app.bpartners.api.service.user.UserService;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImmediateSubscriptionCancellationRequestedService
    implements Consumer<ImmediateSubscriptionCancellationRequested> {
  private final UserService userService;
  private final SubscriptionService subscriptionService;

  @Override
  public void accept(ImmediateSubscriptionCancellationRequested event) {
    var userId = event.getUserId();
    var user = userService.getUserById(userId);
    if (user.getUserSubscriptionId() == null) {
      log.info("User(id={}) has no Stripe subscription, skipping cancellation", userId);
      return;
    }
    subscriptionService.cancelUserSubscriptionsImmediately(user);
    log.info("Cancelled subscriptions immediately for User(id={})", userId);
  }
}
