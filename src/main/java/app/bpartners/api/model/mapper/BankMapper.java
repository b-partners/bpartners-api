package app.bpartners.api.model.mapper;

import app.bpartners.api.model.Bank;
import app.bpartners.api.repository.bridge.model.Bank.BridgeBank;
import org.springframework.stereotype.Component;

@Component
public class BankMapper {
  public Bank toDomain(BridgeBank bridgeBank) {
    return Bank.builder()
        .id(null) //TODO: set persisted ID
        .bridgeBankId(bridgeBank.getId())
        .name(bridgeBank.getName()) //TODO: add parentName if necessary
        .logoUrl(bridgeBank.getLogoUrl())
        .build();
  }
}
