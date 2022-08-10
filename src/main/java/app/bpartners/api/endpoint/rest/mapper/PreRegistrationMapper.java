package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.CreatePreRegistration;
import org.springframework.stereotype.Component;

@Component
public class PreRegistrationMapper {

  public app.bpartners.api.model.PreRegistration toDomainPreregistration(
      CreatePreRegistration createPreRegistration) {
    return app.bpartners.api.model.PreRegistration.builder()
        .email(createPreRegistration.getEmail())
        .firstName(createPreRegistration.getFirstName())
        .lastName(createPreRegistration.getLastName())
        .phoneNumber(createPreRegistration.getPhoneNumber())
        .societyName(createPreRegistration.getSociety())
        .build();
  }
}
