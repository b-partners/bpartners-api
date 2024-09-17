package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.OnboardingRestMapper;
import app.bpartners.api.endpoint.rest.model.VisitorEmail;
import app.bpartners.api.endpoint.rest.model.OnboardUser;
import app.bpartners.api.endpoint.rest.model.OnboardedUser;
import app.bpartners.api.endpoint.rest.model.Redirection;
import app.bpartners.api.endpoint.rest.validator.OnboardingValidator;
import app.bpartners.api.model.exception.NotImplementedException;
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
  private final OnboardingValidator onboardingValidator;
  private final OnboardingRestMapper onboardingMapper;

  @PostMapping(value = "/onboardingInitiation")
  public Redirection generateOnboarding() {
    throw new NotImplementedException("Not supported for now. Use POST /onboarding");
  }

  @PostMapping(value = "/sendEmail")
  public VisitorEmail visitorSendEmail(@RequestBody VisitorEmail emailBody){
    return onboardingService.visitorSendEmail(emailBody);
  }

  @PostMapping(value = "/onboarding")
  public List<OnboardedUser> onboardUser(@RequestBody List<OnboardUser> onboardUsers) {
    onboardingValidator.accept(onboardUsers);
    List<app.bpartners.api.model.OnboardUser> toSave =
        onboardUsers.stream().map(onboardingMapper::toDomain).collect(Collectors.toList());
    return onboardingService.onboardUsers(toSave).stream()
        .map(onboardingMapper::toRest)
        .collect(Collectors.toList());
  }
}
