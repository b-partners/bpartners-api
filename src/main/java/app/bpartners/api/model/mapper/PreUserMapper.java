package app.bpartners.api.model.mapper;


import app.bpartners.api.model.PreUser;
import app.bpartners.api.model.entity.HPreUser;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PreUserMapper {
  public PreUser toDomain(HPreUser hPreUser) {
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

  public HPreUser toEntity(PreUser preUser) {
    HPreUser hPreUser = new HPreUser();
    hPreUser.setEmail(preUser.getEmail());
    hPreUser.setId(preUser.getId());
    hPreUser.setEntranceDatetime(preUser.getEntranceDateTime());
    hPreUser.setSociety(preUser.getSociety());
    hPreUser.setFirstName(preUser.getFirstname());
    hPreUser.setLastName(preUser.getLastname());
    hPreUser.setPhoneNumber(preUser.getMobilePhoneNumber());
    return hPreUser;
  }

  public List<HPreUser> toEntity(List<PreUser> preUser) {
    return preUser.stream()
        .map(this::toEntity)
        .toList();
  }
}
