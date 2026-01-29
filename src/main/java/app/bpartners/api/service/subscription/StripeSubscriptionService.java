package app.bpartners.api.service.subscription;

import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.Subscription;
import com.stripe.param.SubscriptionListParams;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StripeSubscriptionService {
  private final StripeClient stripeClient;

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
