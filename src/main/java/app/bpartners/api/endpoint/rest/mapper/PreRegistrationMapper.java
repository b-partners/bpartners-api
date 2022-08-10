package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.CreatePreRegistration;
import app.bpartners.api.endpoint.rest.model.PreRegistration;
import org.springframework.stereotype.Component;

@Component
public class PreRegistrationMapper {
  public PreRegistration toRestPreRegistration(app.bpartners.api.model.PreRegistration preRegistration) {
    PreRegistration restPreRegistration = new PreRegistration();
    restPreRegistration.setId(preRegistration.getId());
    restPreRegistration.setFirstName(preRegistration.getFirstName());
    restPreRegistration.setLastName(preRegistration.getLastName());
    restPreRegistration.setEmail(preRegistration.getEmail());
    restPreRegistration.setSociety(preRegistration.getSocietyName());
    restPreRegistration.setEntranceDatetime(preRegistration.getEntranceDatetime());
    restPreRegistration.setPhoneNumber(preRegistration.getPhoneNumber());
    return restPreRegistration;
  }

  public app.bpartners.api.model.PreRegistration toDomain(CreatePreRegistration toCreate) {
    app.bpartners.api.model.PreRegistration domainPreRegistration = new app.bpartners.api.model.PreRegistration();
    domainPreRegistration.setFirstName(toCreate.getFirstName());
    domainPreRegistration.setLastName(toCreate.getLastName());
    domainPreRegistration.setEmail(toCreate.getEmail());
    domainPreRegistration.setSocietyName(toCreate.getSociety());
    domainPreRegistration.setPhoneNumber(toCreate.getPhoneNumber());
    return domainPreRegistration;
  }

  public app.bpartners.api.model.PreRegistration toDomain(PreRegistration restPreRegistration) {
    app.bpartners.api.model.PreRegistration domainPreRegistration = new app.bpartners.api.model.PreRegistration();
    domainPreRegistration.setId(restPreRegistration.getId());
    domainPreRegistration.setFirstName(restPreRegistration.getFirstName());
    domainPreRegistration.setLastName(restPreRegistration.getLastName());
    domainPreRegistration.setEmail(restPreRegistration.getEmail());
    domainPreRegistration.setSocietyName(restPreRegistration.getSociety());
    domainPreRegistration.setEntranceDatetime(restPreRegistration.getEntranceDatetime());
    domainPreRegistration.setPhoneNumber(restPreRegistration.getPhoneNumber());
    return domainPreRegistration;
  }
}
