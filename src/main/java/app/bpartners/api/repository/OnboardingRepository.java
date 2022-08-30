package app.bpartners.api.repository;

import app.bpartners.api.model.Onboarding;

public interface OnboardingRepository {
  Onboarding save(String successUrl);
}
