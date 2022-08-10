package app.bpartners.api.endpoint.rest.mapper;


import app.bpartners.api.endpoint.rest.model.CreatePreRegistration;
import app.bpartners.api.model.PreRegistration;
import org.springframework.stereotype.Component;

@Component
public class PreRegistrationMapper {
    public PreRegistration toDomain(CreatePreRegistration create){
        PreRegistration internal = new PreRegistration();
        internal.setEmail(create.getEmail());
        internal.setFirstName(create.getFirstName());
        internal.setLastName(create.getLastName());
        internal.setPhoneNumber(create.getPhoneNumber());
        internal.setSocietyName(create.getSociety());
        return internal;
    }
    public app.bpartners.api.endpoint.rest.model.PreRegistration toRestRegistration(PreRegistration internal){
        app.bpartners.api.endpoint.rest.model.PreRegistration external = new app.bpartners.api.endpoint.rest.model.PreRegistration();
        external.setId(internal.getId());
        external.setFirstName(internal.getFirstName());
        external.setLastName(internal.getLastName());
        external.setEmail(internal.getEmail());
        external.setSociety(internal.getSocietyName());
        external.setEntranceDatetime(internal.getEntranceDatetime());
        external.setPhoneNumber(internal.getPhoneNumber());
        return external;
    }
  public PreRegistration toDomain(app.bpartners.api.endpoint.rest.model.PreRegistration external){
      PreRegistration internal = new PreRegistration();
      internal.setId(external.getId());
      internal.setFirstName(external.getFirstName());
      internal.setLastName(external.getLastName());
      internal.setEmail(external.getEmail());
      internal.setSocietyName(external.getSociety());
      internal.setEntranceDatetime(external.getEntranceDatetime());
      internal.setPhoneNumber(external.getPhoneNumber());
      return internal;
  }
}
