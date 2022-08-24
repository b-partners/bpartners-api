package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.Onboarding;
import app.bpartners.api.repository.OnboardingRepository;
import app.bpartners.api.repository.swan.OnboardingSwanRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class OnboardingRepositoryImpl implements OnboardingRepository {
  private final OnboardingSwanRepository swanRepository;

  @Override
  public Onboarding generateOnboardingUrl(String successUrl) {
    Onboarding newOnboarding = new Onboarding();
    newOnboarding.setOnboardingUrl(swanRepository.getOnboardingUrl(successUrl));
    newOnboarding.setSuccessUrl(successUrl);
    return newOnboarding;
  }
}
