package app.bpartners.api.model.mapper;

import app.bpartners.api.model.SwanUser;
import org.springframework.stereotype.Component;

@Component
public class SwanUserMapper {
  public SwanUser toDomain(app.bpartners.api.repository.swan.schema.SwanUser external) {
    return SwanUser.builder()
        .id(external.id)
        .firstName(external.firstName)
        .lastName(external.lastName)
        .birthDate(external.birthDate)
        .identificationStatus(external.identificationStatus)
        .idVerified(external.idVerified)
        .mobilePhoneNumber(external.mobilePhoneNumber)
        .nationalityCCA3(external.nationalityCCA3)
        .build();
  }
}
