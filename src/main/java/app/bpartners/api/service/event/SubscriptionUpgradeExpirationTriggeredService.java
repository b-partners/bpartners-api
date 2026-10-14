package app.bpartners.api.service.event;

import app.bpartners.api.endpoint.event.model.SubscriptionUpgradeExpirationTriggered;
import app.bpartners.api.service.subscription.SubscriptionUpgradeExpirationService;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SubscriptionUpgradeExpirationTriggeredService
    implements Consumer<SubscriptionUpgradeExpirationTriggered> {
  private final SubscriptionUpgradeExpirationService subscriptionUpgradeExpirationService;

  @Override
  public void accept(SubscriptionUpgradeExpirationTriggered event) {
    subscriptionUpgradeExpirationService.expireOverdue();
  }
}
