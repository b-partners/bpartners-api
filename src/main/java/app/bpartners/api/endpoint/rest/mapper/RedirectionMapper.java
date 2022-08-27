package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.Redirection;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.model.Onboarding;
import org.springframework.stereotype.Component;

@Component
public class RedirectionMapper {
  public Redirection toRest(Onboarding onboarding) {
    Redirection redirection = new Redirection();

    redirection.setRedirectionUrl(onboarding.getOnboardingUrl());

    RedirectionStatusUrls statusUrls = new RedirectionStatusUrls();
    statusUrls.setSuccessUrl(onboarding.getSuccessUrl());
    statusUrls.setFailureUrl(onboarding.getFailureUrl());
    redirection.setRedirectionStatusUrls(statusUrls);

    return redirection;
  }
}
