package app.bpartners.api.service.event;

import static app.bpartners.api.model.subscription.BillingInterval.YEARLY;
import static app.bpartners.api.model.subscription.Subscription.SubscriptionStatus.ACTIVE;
import static app.bpartners.api.model.subscription.Subscription.SubscriptionStatus.CANCELED;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.event.model.UserSubscriptionProductBackfillRequested;
import app.bpartners.api.model.User;
import app.bpartners.api.model.subscription.Subscription;
import app.bpartners.api.model.subscription.UserSubscription;
import app.bpartners.api.service.subscription.SubscriptionService;
import app.bpartners.api.service.subscription.UserSubscriptionProductService;
import app.bpartners.api.service.user.UserService;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.Test;

class UserSubscriptionProductBackfillRequestedServiceTest {
  UserService userService = mock();
  SubscriptionService subscriptionService = mock();
  UserSubscriptionProductService userSubscriptionProductService = mock();
  UserSubscriptionProductBackfillRequestedService subject =
      new UserSubscriptionProductBackfillRequestedService(
          userService, subscriptionService, userSubscriptionProductService);

  private User givenUserWithSubscriptionStatus(Subscription.SubscriptionStatus status) {
    var userId = randomUUID().toString();
    var user = User.builder().id(userId).build();
    when(userService.getUserByIdWithoutPaymentMethod(userId)).thenReturn(user);
    var subscriptions =
        status == null
            ? List.<Subscription>of()
            : List.of(
                Subscription.builder().status(status).active(true).startDatetime(now()).build());
    when(subscriptionService.getSubscriptionByUser(user))
        .thenReturn(UserSubscription.builder().user(user).subscriptions(subscriptions).build());
    return user;
  }

  @Test
  void creates_subscription_product_with_resolved_plan_and_billing_interval_when_status_active() {
    var user = givenUserWithSubscriptionStatus(ACTIVE);
    var planId = "usage_based_plan_id";

    subject.accept(
        UserSubscriptionProductBackfillRequested.builder()
            .userId(user.getId())
            .subscriptionProductId(planId)
            .billingInterval(YEARLY)
            .build());

    verify(userSubscriptionProductService)
        .ensureActiveSubscriptionProduct(user.getId(), planId, YEARLY, null);
  }

  @Test
  void creates_subscription_product_starting_on_the_requested_billing_period_start() {
    var user = givenUserWithSubscriptionStatus(ACTIVE);
    var planId = "essential_plan_id";
    var nextPeriodStart = now().plus(20, ChronoUnit.DAYS);

    subject.accept(
        UserSubscriptionProductBackfillRequested.builder()
            .userId(user.getId())
            .subscriptionProductId(planId)
            .billingInterval(YEARLY)
            .subscriptionStartDatetime(nextPeriodStart)
            .build());

    verify(userSubscriptionProductService)
        .ensureActiveSubscriptionProduct(user.getId(), planId, YEARLY, nextPeriodStart);
  }

  @Test
  void skips_when_status_not_active() {
    var user = givenUserWithSubscriptionStatus(CANCELED);

    subject.accept(UserSubscriptionProductBackfillRequested.builder().userId(user.getId()).build());

    verify(userSubscriptionProductService, never())
        .ensureActiveSubscriptionProduct(any(), any(), any(), any());
  }

  @Test
  void skips_when_no_subscription() {
    var user = givenUserWithSubscriptionStatus(null);

    subject.accept(UserSubscriptionProductBackfillRequested.builder().userId(user.getId()).build());

    verify(userSubscriptionProductService, never())
        .ensureActiveSubscriptionProduct(any(), any(), any(), any());
  }
}
