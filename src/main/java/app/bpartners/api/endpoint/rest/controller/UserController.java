package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.RestUserMapper;
import app.bpartners.api.endpoint.rest.model.OnboardingParams;
import app.bpartners.api.endpoint.rest.model.RedirectionComponent;
import app.bpartners.api.endpoint.rest.model.User;
import app.bpartners.api.service.UserService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class UserController {
  private final UserService service;
  private final RestUserMapper mapper;

  @GetMapping("/users/{id}")
  public User getUserById(@PathVariable String id) {
    return mapper.toRest(service.getUserById(id));
  }

  @GetMapping("/onboarding")
  public RedirectionComponent redirectOnboardingUrl(OnboardingParams params) {
    return service.generateOnboardingUrl(params);
  }
}
