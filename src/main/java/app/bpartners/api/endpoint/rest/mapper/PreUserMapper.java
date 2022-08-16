package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.CreatePreUser;
import app.bpartners.api.endpoint.rest.model.PreUser;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class PreUserMapper {

  public app.bpartners.api.model.PreUser toDomain(
      CreatePreUser createPreUser) {
    return app.bpartners.api.model.PreUser.builder()
        .email(createPreUser.getEmail())
        .firstName(createPreUser.getFirstName())
        .lastName(createPreUser.getLastName())
        .phoneNumber(createPreUser.getPhoneNumber())
        .society(createPreUser.getSociety())
        .build();
  }

  public PreUser toRest(app.bpartners.api.model.PreUser internal) {
    PreUser preRegistration = new PreUser();
    preRegistration.setId(internal.getId());
    preRegistration.setEmail(internal.getEmail());
    preRegistration.setEntranceDatetime(internal.getEntranceDatetime());
    preRegistration.setFirstName(internal.getFirstName());
    preRegistration.setLastName(internal.getLastName());
    preRegistration.setPhoneNumber(internal.getPhoneNumber());
    preRegistration.setSociety(internal.getSociety());
    return preRegistration;
  }

  public List<app.bpartners.api.model.PreUser> toDomain(List<CreatePreUser> preUsers) {
    return preUsers.stream()
        .map(this::toDomain)
        .collect(Collectors.toUnmodifiableList());
  }

  public List<PreUser> toRest(List<app.bpartners.api.model.PreUser> preUsers) {
    return preUsers.stream()
        .map(this::toRest)
        .collect(Collectors.toUnmodifiableList());
  }
}
