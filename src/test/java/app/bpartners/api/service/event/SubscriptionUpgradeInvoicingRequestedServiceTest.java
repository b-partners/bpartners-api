package app.bpartners.api.service.event;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import app.bpartners.api.endpoint.event.model.SubscriptionUpgradeInvoicingRequested;
import app.bpartners.api.service.subscription.SubscriptionUpgradeInvoicingService;
import org.junit.jupiter.api.Test;

class SubscriptionUpgradeInvoicingRequestedServiceTest {
  SubscriptionUpgradeInvoicingService invoicingService = mock();
  SubscriptionUpgradeInvoicingRequestedService subject =
      new SubscriptionUpgradeInvoicingRequestedService(invoicingService);

  @Test
  void invoices_the_requested_upgrade() {
    subject.accept(
        SubscriptionUpgradeInvoicingRequested.builder()
            .subscriptionUpgradeId("upgrade_id")
            .build());

    verify(invoicingService).invoice("upgrade_id");
  }
}
