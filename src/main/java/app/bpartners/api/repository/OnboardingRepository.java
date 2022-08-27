package app.bpartners.api.repository;

import app.bpartners.api.model.Onboarding;

public interface OnboardingRepository {
  //TODO(repository-functions): does not sound like a name of a repository function
  Onboarding generateOnboardingUrl(String successUrl);
}
