package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.CreatePreUser;
import app.bpartners.api.endpoint.rest.model.PreUser;
import app.bpartners.api.endpoint.rest.validator.PreUserRestValidator;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class PreUserRestMapper {

  private final PreUserRestValidator preUserValidator;

  public app.bpartners.api.model.PreUser toDomain(CreatePreUser toCreate) {
    preUserValidator.accept(toCreate);
    return app.bpartners.api.model.PreUser.builder()
        .firstname(toCreate.getFirstName())
        .lastname(toCreate.getLastName())
        .email(toCreate.getEmail())
        .society(toCreate.getSociety())
        .mobilePhoneNumber(toCreate.getPhone())
        .build();
  }

  public PreUser toRest(app.bpartners.api.model.PreUser preUser) {
    return new PreUser()
        .id(preUser.getId())
        .firstName(preUser.getFirstname())
        .lastName(preUser.getLastname())
        .phone(preUser.getMobilePhoneNumber())
        .society(preUser.getSociety())
        .email(preUser.getEmail())
        .entranceDatetime(preUser.getEntranceDateTime());
  }
}
