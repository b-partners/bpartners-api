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
        .name(entity.getName())
        .logoUrl(entity.getLogoUrl())
        .build();
  }

  public HBank toEntity(Bank bank) {
    if (bank == null) {
      return null;
    }
    return HBank.builder().id(bank.getId()).name(bank.getName()).logoUrl(bank.getLogoUrl()).build();
  }
}
