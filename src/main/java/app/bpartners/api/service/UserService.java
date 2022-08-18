package app.bpartners.api.service;

import app.bpartners.api.endpoint.rest.model.OnboardingParams;
import app.bpartners.api.endpoint.rest.model.RedirectionComponent;
import app.bpartners.api.model.User;
import app.bpartners.api.model.entity.HUser;
import app.bpartners.api.model.entity.validator.OnboardingValidator;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.jpa.UserJpaRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UserService {
  private UserJpaRepository userJpaRepository;

  private UserRepository userRepository;
  private OnboardingValidator onboardingValidator;

  public User getUserById(String userId) {
    return userRepository.getUserById(userId);
  }

  public User getUserBySwanId(String swanUserId) {
    return userRepository.getUserBySwanUserId(swanUserId);
  }

  public RedirectionComponent generateOnboardingUrl(OnboardingParams params) {
    onboardingValidator.accept(params);
    RedirectionComponent redirectionComponent = new RedirectionComponent();
    redirectionComponent.setRedirectionUrl("TODO");
    redirectionComponent.setOnSuccessUrl(params.getOnSuccessUrl());
    redirectionComponent.setOnFailUrl(params.getOnFailUrl());
    return redirectionComponent;
  }
}
