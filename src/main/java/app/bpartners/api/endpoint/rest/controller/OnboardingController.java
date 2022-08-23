package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.RedirectionMapper;
import app.bpartners.api.endpoint.rest.model.OnboardingParams;
import app.bpartners.api.endpoint.rest.model.RedirectionComponent;
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

  @PostMapping(value = "/onboarding")
  public RedirectionComponent generateOnboarding(@RequestBody OnboardingParams params) {
    return mapper.toRest(
        onboardingService.generateOnboarding(params.getSuccessUrl(), params.getFailureUrl()));
  }
}
