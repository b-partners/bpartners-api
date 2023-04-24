package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.RedirectionMapper;
import app.bpartners.api.endpoint.rest.mapper.UserRestMapper;
import app.bpartners.api.endpoint.rest.model.OnboardUser;
import app.bpartners.api.endpoint.rest.model.OnboardingInitiation;
import app.bpartners.api.endpoint.rest.model.Redirection;
import app.bpartners.api.endpoint.rest.validator.OnboardingInitiationValidator;
import app.bpartners.api.endpoint.rest.validator.OnboardingValidator;
import app.bpartners.api.service.OnboardingService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class OnboardingController {
  private final OnboardingService onboardingService;
  private final RedirectionMapper mapper;
  private final OnboardingInitiationValidator initiationValidator;
  private final UserRestMapper userMapper;
  private final OnboardingValidator onboardingValidator;

  @PostMapping(value = "/onboardingInitiation")
  public Redirection generateOnboarding(@RequestBody OnboardingInitiation params) {
    initiationValidator.accept(params);
    return mapper.toRest(
        onboardingService.generateOnboarding(params.getRedirectionStatusUrls().getSuccessUrl(),
            params.getRedirectionStatusUrls().getFailureUrl()));
  }

  @PostMapping(value = "/onboarding", produces = "text/plain")
  public String onboardUser(@RequestBody OnboardUser toCreateUser) {
    onboardingValidator.accept(toCreateUser);
    app.bpartners.api.model.User toSave = userMapper.toDomain(toCreateUser);
    onboardingService.onboardUser(toSave, toCreateUser.getCompanyName());
    return "User is created";
  }
}
