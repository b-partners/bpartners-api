package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.CreatePreUser;
import app.bpartners.api.endpoint.rest.model.PreUser;
import app.bpartners.api.model.entity.HPreUser;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class PreUserMapper {

  public HPreUser toDomain(
      CreatePreUser createPreUser) {
    return HPreUser.builder()
        .email(createPreUser.getEmail())
        .firstName(createPreUser.getFirstName())
        .lastName(createPreUser.getLastName())
        .phoneNumber(createPreUser.getPhoneNumber())
        .society(createPreUser.getSociety())
        .build();
  }

  public List<HPreUser> toDomain(List<CreatePreUser> toCreate) {
    return toCreate.stream()
        .map(this::toDomain)
        .collect(Collectors.toUnmodifiableList());
  }

  public PreUser toRest(HPreUser hPreUser) {
    PreUser restPreUser = new PreUser();
    restPreUser.setId(hPreUser.getId());
    restPreUser.setEmail(hPreUser.getEmail());
    restPreUser.setEntranceDatetime(hPreUser.getEntranceDatetime());
    restPreUser.setFirstName(hPreUser.getFirstName());
    restPreUser.setLastName(hPreUser.getLastName());
    restPreUser.setPhoneNumber(hPreUser.getPhoneNumber());
    restPreUser.setSociety(hPreUser.getSociety());
    return restPreUser;
  }

  public List<PreUser> toRest(List<HPreUser> hPreUsers) {
    return hPreUsers.stream()
        .map(this::toRest)
        .collect(Collectors.toUnmodifiableList());
  }
}
