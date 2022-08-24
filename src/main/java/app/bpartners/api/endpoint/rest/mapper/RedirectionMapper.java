package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.RedirectionComponent;
import app.bpartners.api.model.Onboarding;
import org.springframework.stereotype.Component;

@Component
public class RedirectionMapper {
  public RedirectionComponent toRest(Onboarding onboarding) {
    RedirectionComponent redirectionComponent = new RedirectionComponent();
    redirectionComponent.setRedirectionUrl(onboarding.getOnboardingUrl());
    redirectionComponent.setFailureUrl(onboarding.getFailureUrl());
    redirectionComponent.setSuccessUrl(onboarding.getSuccessUrl());
    return redirectionComponent;
  }
}
