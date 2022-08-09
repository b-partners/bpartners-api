package app.bpartners.api.endpoint.rest.controller;


import app.bpartners.api.endpoint.rest.mapper.PreRegistrationMapper;
import app.bpartners.api.endpoint.rest.model.CreatePreRegistration;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.service.PreRegistrationService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class PreRegistrationController {

  private final PreRegistrationService preRegistrationService;

  private final PreRegistrationMapper preRegistrationMapper;

  @PostMapping("/pre-registration")
  public String createEmail(@RequestBody CreatePreRegistration toCreate) {
    throw new NotImplementedException("Not implemented");
  }
}
