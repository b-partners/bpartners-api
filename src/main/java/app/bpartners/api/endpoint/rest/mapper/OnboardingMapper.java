package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.OnboardingInitiation;
import app.bpartners.api.endpoint.rest.validator.OnboardingValidator;
import app.bpartners.api.model.Onboarding;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class OnboardingMapper {

  private final OnboardingValidator onboardingValidator;

  public Onboarding toDomain(OnboardingInitiation restOnboarding) {
    onboardingValidator.accept(restOnboarding);
    return Onboarding
        .builder()
        .successUrl(String.valueOf(restOnboarding.getRedirectionStatusUrls().getSuccessUrl()))
        .failureUrl(String.valueOf(restOnboarding.getRedirectionStatusUrls().getFailureUrl()))
        .build();
  }

}
