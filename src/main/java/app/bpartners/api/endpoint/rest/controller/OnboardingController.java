package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.OnboardingRestMapper;
import app.bpartners.api.endpoint.rest.mapper.RedirectionMapper;
import app.bpartners.api.endpoint.rest.mapper.UserRestMapper;
import app.bpartners.api.endpoint.rest.model.OnboardUser;
import app.bpartners.api.endpoint.rest.model.OnboardedUser;
import app.bpartners.api.endpoint.rest.model.OnboardingInitiation;
import app.bpartners.api.endpoint.rest.model.Redirection;
import app.bpartners.api.endpoint.rest.validator.OnboardingInitiationValidator;
import app.bpartners.api.endpoint.rest.validator.OnboardingValidator;
import app.bpartners.api.service.OnboardingService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class OnboardingController {
  private final OnboardingService onboardingService;
  private final RedirectionMapper redirectionMapper;
  private final OnboardingInitiationValidator initiationValidator;
  private final UserRestMapper userMapper;
  private final OnboardingValidator onboardingValidator;
  private final OnboardingRestMapper onboardingMapper;

  @PostMapping(value = "/onboardingInitiation")
  public Redirection generateOnboarding(@RequestBody OnboardingInitiation params) {
    initiationValidator.accept(params);
    return redirectionMapper.toRest(
        onboardingService.generateOnboarding(params.getRedirectionStatusUrls().getSuccessUrl(),
            params.getRedirectionStatusUrls().getFailureUrl()));
  }

  @PostMapping(value = "/onboarding")
  public List<OnboardedUser> onboardUser(@RequestBody List<OnboardUser> onboardUsers) {
    onboardingValidator.accept(onboardUsers);
    List<app.bpartners.api.model.OnboardUser> toSave = onboardUsers.stream()
        .map(onboardingMapper::toDomain)
        .collect(Collectors.toList());
    return onboardingService.onboardUsers(toSave).stream()
        .map(onboardingMapper::toRest)
        .collect(Collectors.toList());
  }
}
