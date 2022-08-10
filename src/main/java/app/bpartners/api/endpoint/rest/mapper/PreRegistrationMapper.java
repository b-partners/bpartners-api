package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.CreatePreRegistration;
import app.bpartners.api.endpoint.rest.model.PreRegistration;
import org.springframework.stereotype.Component;

@Component
public class PreRegistrationMapper {

  public static PreRegistration toRestPreRegistration(app.bpartners.api.model.PreRegistration preRegistration){
    PreRegistration restPreRegistration = new PreRegistration();
    restPreRegistration.setId(preRegistration.getId());
    restPreRegistration.setFirstName(preRegistration.getFirstName());
    restPreRegistration.setLastName(preRegistration.getLastName());
    restPreRegistration.setSociety(preRegistration.getSocietyName());
    restPreRegistration.setEmail(preRegistration.getEmail());
    restPreRegistration.setEntranceDatetime(preRegistration.getEntranceDatetime());
    restPreRegistration.setPhoneNumber(preRegistration.getPhoneNumber());
    return restPreRegistration;
  }

  public app.bpartners.api.model.PreRegistration toDomain(PreRegistration preRegistration){
    return app.bpartners.api.model.PreRegistration.builder()
            .id(preRegistration.getId())
            .firstName(preRegistration.getFirstName())
            .lastName(preRegistration.getLastName())
            .societyName(preRegistration.getSociety())
            .email(preRegistration.getEmail())
            .entranceDatetime(preRegistration.getEntranceDatetime())
            .phoneNumber(preRegistration.getPhoneNumber())
            .build();
  }
  public app.bpartners.api.model.PreRegistration toDomain(CreatePreRegistration preRegistration){
    return app.bpartners.api.model.PreRegistration.builder()
            .firstName(preRegistration.getFirstName())
            .lastName(preRegistration.getLastName())
            .societyName(preRegistration.getSociety())
            .email(preRegistration.getEmail())
            .phoneNumber(preRegistration.getPhoneNumber())
            .build();
  }
}
