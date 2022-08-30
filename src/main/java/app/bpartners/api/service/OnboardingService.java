package app.bpartners.api.service;

import app.bpartners.api.model.Onboarding;
import app.bpartners.api.repository.OnboardingRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class OnboardingService {
  private final OnboardingRepository repository;

  public Onboarding generateOnboarding(String successUrl, String failureUrl) {
    Onboarding onboarding = repository.save(successUrl);
    onboarding.setFailureUrl(failureUrl);
    return onboarding;
  }
}
