package app.bpartners.api.service.event;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.event.model.ImmediateSubscriptionCancellationRequested;
import app.bpartners.api.model.User;
import app.bpartners.api.service.subscription.SubscriptionService;
import app.bpartners.api.service.user.UserService;
import org.junit.jupiter.api.Test;

class ImmediateSubscriptionCancellationRequestedServiceTest {
  UserService userService = mock();
  SubscriptionService subscriptionService = mock();
  ImmediateSubscriptionCancellationRequestedService subject =
      new ImmediateSubscriptionCancellationRequestedService(userService, subscriptionService);

  @Test
  void cancels_the_subscriptions_immediately() {
    var user = User.builder().id("user_id").userSubscriptionId("stripe_customer_id").build();
    when(userService.getUserById("user_id")).thenReturn(user);

    subject.accept(ImmediateSubscriptionCancellationRequested.builder().userId("user_id").build());

    verify(subscriptionService).cancelUserSubscriptionsImmediately(user);
  }

  @Test
  void skips_when_user_has_no_stripe_subscription() {
    var user = User.builder().id("user_id").build();
    when(userService.getUserById("user_id")).thenReturn(user);

    subject.accept(ImmediateSubscriptionCancellationRequested.builder().userId("user_id").build());

    verify(subscriptionService, never()).cancelUserSubscriptionsImmediately(any());
  }
}
