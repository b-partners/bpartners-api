package app.bpartners.api.model.mapper;

import app.bpartners.api.model.SwanUser;
import org.springframework.stereotype.Component;

@Component
public class SwanUserMapper {
  public SwanUser toDomain(app.bpartners.api.repository.swan.schema.SwanUser external) {
    return SwanUser.builder()
        .id(external.getId())
        .firstName(external.getFirstName())
        .lastName(external.getLastName())
        .birthDate(external.getBirthDate())
        .identificationStatus(external.getIdentificationStatus())
        .idVerified(external.getIdVerified())
        .mobilePhoneNumber(external.getMobilePhoneNumber())
        .nationalityCCA3(external.getNationalityCCA3())
        .build();
  }
}
