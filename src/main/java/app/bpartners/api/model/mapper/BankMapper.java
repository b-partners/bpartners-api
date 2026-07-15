package app.bpartners.api.model.mapper;

import app.bpartners.api.model.Bank;
import app.bpartners.api.repository.jpa.model.HBank;
import org.springframework.stereotype.Component;

@Component
public class BankMapper {
  public Bank toDomain(HBank entity) {
    if (entity == null) {
      return null;
    }
    return Bank.builder()
        .id(entity.getId())
        .externalId(entity.getExternalId())
        .name(entity.getName())
        .logoUrl(entity.getLogoUrl())
        .build();
  }
}
