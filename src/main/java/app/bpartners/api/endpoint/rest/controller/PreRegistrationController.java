package app.bpartners.api.endpoint.rest.controller;


import app.bpartners.api.endpoint.rest.mapper.PreRegistrationMapper;
import app.bpartners.api.endpoint.rest.model.CreatePreRegistration;
import app.bpartners.api.endpoint.rest.model.PreRegistration;
import app.bpartners.api.service.PreRegistrationService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@AllArgsConstructor
public class PreRegistrationController {

  private final PreRegistrationService preRegistrationService;

  private final PreRegistrationMapper preRegistrationMapper;

  @GetMapping("/pre-registration")
  public List<PreRegistration> getAllPreRegistration() {
    return preRegistrationService.getPreRegistration()
            .stream().map(PreRegistrationMapper::toRestPreRegistration)
            .collect(Collectors.toUnmodifiableList());
  }

  @PostMapping("/pre-registration")
  public PreRegistration createPregistration(@RequestBody CreatePreRegistration toCreate) {
    app.bpartners.api.model.PreRegistration preRegistration = preRegistrationService.createPreRegistration(preRegistrationMapper.toDomain(toCreate));
    return PreRegistrationMapper.toRestPreRegistration(preRegistration);
  }
}
