package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.CreatePreUser;
import app.bpartners.api.endpoint.rest.model.PreUser;
import app.bpartners.api.endpoint.rest.validator.PreUserValidators;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class PreUserRestMapper {

  private final PreUserValidators preUserValidator;

  public app.bpartners.api.model.PreUser toDomain(
      CreatePreUser toCreate) {
    preUserValidator.accept(toCreate);
    return app.bpartners.api.model.PreUser.builder()
        .firstname(toCreate.getFirstName())
        .lastname(toCreate.getFirstName())
        .email(toCreate.getEmail())
        .society(toCreate.getSociety())
        .mobilePhoneNumber(toCreate.getPhone())
        .build();
  }

  public PreUser toRest(app.bpartners.api.model.PreUser preUser) {
    PreUser restPreUser = new PreUser();
    restPreUser.setId(preUser.getId());
    restPreUser.setFirstName(preUser.getFirstname());
    restPreUser.setLastName(preUser.getLastname());
    restPreUser.setPhone(preUser.getMobilePhoneNumber());
    restPreUser.setSociety(preUser.getSociety());
    restPreUser.setEmail(preUser.getEmail());
    restPreUser.setEntranceDatetime(preUser.getEntranceDateTime());
    return restPreUser;
  }
}
