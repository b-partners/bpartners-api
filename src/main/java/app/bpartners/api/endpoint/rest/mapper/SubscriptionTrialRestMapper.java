package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.SubscriptionTrial;
import app.bpartners.api.model.subscription.StartedSubscriptionTrial;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionTrialRestMapper {

  public SubscriptionTrial toRest(StartedSubscriptionTrial domain) {
    var trial = domain.trial();
    return new SubscriptionTrial()
        .subscriptionPlanIdentifier(trial.getSubscriptionProductId())
        .startedAt(trial.getStartedAt())
        .expiresAt(trial.getExpiresAt())
        .grantedAnalyses(Math.toIntExact(domain.grantedAnalyses()))
        .grantedCredits(domain.grantedCredits());
  }
}
