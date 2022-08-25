package app.bpartners.api.model.mapper;

import app.bpartners.api.model.AccountHolder;
import org.springframework.stereotype.Component;

@Component
public class AccountHolderMapper {
  public AccountHolder toDomain(
      app.bpartners.api.repository.swan.schema.AccountHolder accountHolder) {
    return AccountHolder.builder()
        .id(accountHolder.id)
        .name(accountHolder.info.name)
        .address(accountHolder.residencyAddress.addressLine1)
        .city(accountHolder.residencyAddress.city)
        .country(accountHolder.residencyAddress.country)
        .postalCode(accountHolder.residencyAddress.postalCode)
        .build();
  }
}
