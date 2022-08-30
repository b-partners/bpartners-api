package app.bpartners.api.model.mapper;


import app.bpartners.api.model.PreUser;
import app.bpartners.api.repository.jpa.model.HPreUser;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class PreUserMapper {
  public PreUser toDomain(HPreUser entityPreUser) {
    return PreUser.builder()
        .id(entityPreUser.getId())
        .email(entityPreUser.getEmail())
        .entranceDateTime(entityPreUser.getEntranceDatetime())
        .firstname(entityPreUser.getFirstName())
        .lastname(entityPreUser.getLastName())
        .mobilePhoneNumber(entityPreUser.getPhoneNumber())
        .society(entityPreUser.getSociety())
        .build();
  }

  public HPreUser toEntity(PreUser preUser) {
    HPreUser entityPreUser = new HPreUser();
    entityPreUser.setEmail(preUser.getEmail());
    entityPreUser.setId(preUser.getId());
    entityPreUser.setEntranceDatetime(preUser.getEntranceDateTime());
    entityPreUser.setSociety(preUser.getSociety());
    entityPreUser.setFirstName(preUser.getFirstname());
    entityPreUser.setLastName(preUser.getLastname());
    entityPreUser.setPhoneNumber(preUser.getMobilePhoneNumber());
    return entityPreUser;
  }

  public List<HPreUser> toEntity(List<PreUser> preUser) {
    return preUser.stream()
        .map(this::toEntity)
        .collect(Collectors.toUnmodifiableList());
  }
}
