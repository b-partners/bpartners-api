package app.bpartners.api.repository;

import app.bpartners.api.model.Onboarding;

public interface OnboardingRepository {
  Onboarding generateOnboardingUrl(String successUrl);
}
