package app.bpartners.api.service.event;

import app.bpartners.api.endpoint.event.model.SubscriptionUpgradeInvoicingRequested;
import app.bpartners.api.service.subscription.SubscriptionUpgradeInvoicingService;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriptionUpgradeInvoicingRequestedService
    implements Consumer<SubscriptionUpgradeInvoicingRequested> {
  private final SubscriptionUpgradeInvoicingService subscriptionUpgradeInvoicingService;

  @Override
  public void accept(SubscriptionUpgradeInvoicingRequested event) {
    subscriptionUpgradeInvoicingService.invoice(event.getSubscriptionUpgradeId());
  }
}
