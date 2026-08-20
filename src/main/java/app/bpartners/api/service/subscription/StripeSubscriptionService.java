package app.bpartners.api.service.subscription;

import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.Subscription;
import com.stripe.param.SubscriptionListParams;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StripeSubscriptionService {
  private static final Set<String> TERMINATED_STATUSES = Set.of("canceled", "incomplete_expired");
  private final StripeClient stripeClient;

  public static boolean isTerminated(Subscription stripeSubscription) {
    return TERMINATED_STATUSES.contains(stripeSubscription.getStatus());
  }

  public List<Subscription> getStripeSubscriptionsFromStripeCustomerId(String stripeCustomerId)
      throws StripeException {
    List<com.stripe.model.Subscription> stripeSubscriptions;
    stripeSubscriptions =
        stripeClient
            .subscriptions()
            .list(
                SubscriptionListParams.builder()
                    .setCustomer(stripeCustomerId)
                    .setStatus(SubscriptionListParams.Status.ALL)
                    .build())
            .getData();
    return stripeSubscriptions;
  }
}
