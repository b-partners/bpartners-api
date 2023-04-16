package app.bpartners.api.model.mapper;

import app.bpartners.api.model.Bank;
import app.bpartners.api.repository.bridge.model.Bank.BridgeBank;
import app.bpartners.api.repository.jpa.model.HBank;
import org.springframework.stereotype.Component;

@Component
public class BankMapper {
  public Bank toDomain(HBank entity, BridgeBank bridgeBank) {
    if (entity == null) {
      return null;
    }
    return Bank.builder()
        .id(entity.getId())
        .externalId(bridgeBank != null ? bridgeBank.getId() : entity.getExternalId())
        .name(bridgeBank != null ? bridgeBank.getName() : entity.getName())
        .logoUrl(bridgeBank != null ? bridgeBank.getLogoUrl() : entity.getLogoUrl())
        .build();
  }

  public HBank toEntity(BridgeBank bridgeBank) {
    return HBank.builder()
        .name(bridgeBank.getName())
        .externalId(bridgeBank.getId())
        .logoUrl(bridgeBank.getLogoUrl())
        .build();
  }
}
