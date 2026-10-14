package app.bpartners.api.service.event;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import app.bpartners.api.endpoint.event.model.SubscriptionUpgradeExpirationTriggered;
import app.bpartners.api.service.subscription.SubscriptionUpgradeExpirationService;
import org.junit.jupiter.api.Test;

class SubscriptionUpgradeExpirationTriggeredServiceTest {
  SubscriptionUpgradeExpirationService expirationService = mock();
  SubscriptionUpgradeExpirationTriggeredService subject =
      new SubscriptionUpgradeExpirationTriggeredService(expirationService);

  @Test
  void expires_the_overdue_upgrades() {
    subject.accept(new SubscriptionUpgradeExpirationTriggered());

    verify(expirationService).expireOverdue();
  }
}
