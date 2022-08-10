package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.CreatePreRegistration;
import app.bpartners.api.endpoint.rest.model.PreRegistration;
import org.springframework.stereotype.Component;

@Component
public class PreRegistrationMapper {

  public app.bpartners.api.model.PreRegistration toDomain(
      CreatePreRegistration createPreRegistration) {
    return app.bpartners.api.model.PreRegistration.builder()
        .email(createPreRegistration.getEmail())
        .firstName(createPreRegistration.getFirstName())
        .lastName(createPreRegistration.getLastName())
        .phoneNumber(createPreRegistration.getPhoneNumber())
        .society(createPreRegistration.getSociety())
        .build();
  }

  public PreRegistration toRest(app.bpartners.api.model.PreRegistration internal) {
    PreRegistration preRegistration = new PreRegistration();
    preRegistration.setId(internal.getId());
    preRegistration.setEmail(internal.getEmail());
    preRegistration.setEntranceDatetime(internal.getEntranceDatetime());
    preRegistration.setFirstName(internal.getFirstName());
    preRegistration.setLastName(internal.getLastName());
    preRegistration.setPhoneNumber(internal.getPhoneNumber());
    preRegistration.setSociety(internal.getSociety());
    return preRegistration;
  }
}
