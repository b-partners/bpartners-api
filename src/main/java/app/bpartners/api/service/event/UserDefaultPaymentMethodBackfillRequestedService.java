package app.bpartners.api.service.event;

import app.bpartners.api.endpoint.event.model.UserDefaultPaymentMethodBackfillRequested;
import app.bpartners.api.service.subscription.StripeDefaultPaymentMethodService;
import app.bpartners.api.service.user.UserService;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserDefaultPaymentMethodBackfillRequestedService
    implements Consumer<UserDefaultPaymentMethodBackfillRequested> {
  private final UserService userService;
  private final StripeDefaultPaymentMethodService stripeDefaultPaymentMethodService;

  @Override
  public void accept(UserDefaultPaymentMethodBackfillRequested event) {
    var user = userService.getUserByIdWithoutPaymentMethod(event.getUserId());
    var stripeCustomerIdentifier = user.getUserSubscriptionId();
    if (stripeCustomerIdentifier == null) {
      log.info(
          "User(id={}) is not associated to a Stripe customer, skipping default payment method"
              + " backfill",
          user.getId());
      return;
    }
    stripeDefaultPaymentMethodService
        .ensureDefaultPaymentMethod(stripeCustomerIdentifier)
        .ifPresentOrElse(
            defaultPaymentMethodId ->
                log.info(
                    "User(id={}) now has PaymentMethod.id={} as default",
                    user.getId(),
                    defaultPaymentMethodId),
            () -> log.info("User(id={}) has no payment method to set as default", user.getId()));
  }
}
