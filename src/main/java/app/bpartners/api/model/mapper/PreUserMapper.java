package app.bpartners.api.model.mapper;


import app.bpartners.api.model.PreUser;
import app.bpartners.api.model.entity.HPreUser;

public class PreUserMapper {
  public PreUser HtoDomain(HPreUser hPreUser) {
    PreUser preUser = PreUser.builder()
        .id(hPreUser.getId())
        .email(hPreUser.getEmail())
        .entranceDateTime(hPreUser.getEntranceDatetime())
        .firstname(hPreUser.getFirstName())
        .lastname(hPreUser.getLastName())
        .mobilePhoneNumber(hPreUser.getPhoneNumber())
        .society(hPreUser.getSociety())
        .build();
    return preUser;
  }

  public app.bpartners.api.endpoint.rest.model.PreUser DtoRest(PreUser preUser) {
    app.bpartners.api.endpoint.rest.model.PreUser restPreUser =
        new app.bpartners.api.endpoint.rest.model.PreUser();
    restPreUser.setEmail(preUser.getEmail());
    restPreUser.setId(preUser.getId());
    restPreUser.setEntranceDatetime(preUser.getEntranceDateTime());
    restPreUser.setSociety(preUser.getSociety());
    restPreUser.setFirstName(preUser.getFirstname());
    restPreUser.setLastName(preUser.getLastname());
    restPreUser.setPhoneNumber(preUser.getMobilePhoneNumber());
    return restPreUser;
  }
}
