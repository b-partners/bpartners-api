package app.bpartners.api.service.event;

import app.bpartners.api.endpoint.event.model.SubscriptionProductStripeVatBackfillTriggered;
import app.bpartners.api.service.subscription.SubscriptionService;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionProductStripeVatBackfillTriggeredService
    implements Consumer<SubscriptionProductStripeVatBackfillTriggered> {
  private final SubscriptionService subscriptionService;

  @Override
  public void accept(SubscriptionProductStripeVatBackfillTriggered event) {
    subscriptionService.backfillStripeProductsVatMetadata();
  }
}
