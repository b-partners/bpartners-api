package app.bpartners.api.endpoint.rest.controller;


import app.bpartners.api.endpoint.rest.mapper.PreRegistrationMapper;
import app.bpartners.api.endpoint.rest.model.CreatePreRegistration;
import app.bpartners.api.endpoint.rest.model.PreRegistration;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.validator.PreRegistrationValidator;
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

  private final PreRegistrationValidator validator;

  @PostMapping("/pre-registration")
  public PreRegistration createPreRegistration(@RequestBody CreatePreRegistration toCreate) {
    if (validator.accept(toCreate)) {
      app.bpartners.api.model.PreRegistration createdPreRegistration = preRegistrationMapper
              .toDomain(toCreate);
      return preRegistrationMapper
              .toRestPreRegistration(preRegistrationService
                      .createPreRegistration(createdPreRegistration));
    } else {
      throw new BadRequestException("Invalid email format");
    }
  }
}
