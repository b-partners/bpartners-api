package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.SubscriptionTrialEligibility;
import app.bpartners.api.endpoint.rest.model.SubscriptionTrialIneligibilityReason;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionTrialEligibilityRestMapper {

  public SubscriptionTrialEligibility toRest(
      app.bpartners.api.model.subscription.SubscriptionTrialEligibility domain) {
    return new SubscriptionTrialEligibility()
        .subscriptionPlanIdentifier(domain.subscriptionProductId())
        .eligible(domain.eligible())
        .reason(SubscriptionTrialIneligibilityReason.valueOf(domain.reason().name()));
  }
}
