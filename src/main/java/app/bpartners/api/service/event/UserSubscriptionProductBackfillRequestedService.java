package app.bpartners.api.service.event;

import app.bpartners.api.endpoint.event.model.UserSubscriptionProductBackfillRequested;
import app.bpartners.api.model.User;
import app.bpartners.api.service.subscription.SubscriptionService;
import app.bpartners.api.service.subscription.UserSubscriptionProductService;
import app.bpartners.api.service.user.UserService;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserSubscriptionProductBackfillRequestedService
    implements Consumer<UserSubscriptionProductBackfillRequested> {
  private final UserService userService;
  private final SubscriptionService subscriptionService;
  private final UserSubscriptionProductService userSubscriptionProductService;

  @Override
  public void accept(UserSubscriptionProductBackfillRequested event) {
    var user = userService.getUserByIdWithoutPaymentMethod(event.getUserId());
    if (!hasActiveSubscription(user)) {
      log.info(
          "User(id={}) has no ACTIVE subscription, skipping UserSubscriptionProduct backfill",
          user.getId());
      return;
    }
    userSubscriptionProductService.ensureActiveSubscriptionProduct(
        user.getId(),
        event.getSubscriptionProductId(),
        event.getBillingInterval(),
        event.getSubscriptionStartDatetime());
  }

  private boolean hasActiveSubscription(User user) {
    return subscriptionService.getSubscriptionByUser(user).hasActivePaidSubscription();
  }
}
