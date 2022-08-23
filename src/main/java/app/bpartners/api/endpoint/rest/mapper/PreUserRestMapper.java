package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.CreatePreUser;
import app.bpartners.api.endpoint.rest.model.PreUser;
import org.springframework.stereotype.Component;

@Component
public class PreUserRestMapper {

  public app.bpartners.api.model.PreUser toDomain(
      CreatePreUser toCreate) {
    return app.bpartners.api.model.PreUser.builder()
        .firstname(toCreate.getFirstName())
        .lastname(toCreate.getFirstName())
        .email(toCreate.getEmail())
        .society(toCreate.getSociety())
        .mobilePhoneNumber(toCreate.getPhoneNumber())
        .build();
  }

  public PreUser toRest(app.bpartners.api.model.PreUser preUser) {
    PreUser restPreUser = new PreUser();
    restPreUser.setId(preUser.getId());
    restPreUser.setFirstName(preUser.getFirstname());
    restPreUser.setLastName(preUser.getLastname());
    restPreUser.setPhoneNumber(preUser.getMobilePhoneNumber());
    restPreUser.setSociety(preUser.getSociety());
    restPreUser.setEmail(preUser.getEmail());
    restPreUser.setEntranceDatetime(preUser.getEntranceDateTime());
    return restPreUser;
  }
}
