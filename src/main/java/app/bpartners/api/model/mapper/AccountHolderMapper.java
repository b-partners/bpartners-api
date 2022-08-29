package app.bpartners.api.model.mapper;

import app.bpartners.api.model.AccountHolder;
import org.springframework.stereotype.Component;

@Component
public class AccountHolderMapper {
  public AccountHolder toDomain(
      app.bpartners.api.repository.swan.schema.AccountHolder accountHolder) {
    return AccountHolder.builder()
        .id(accountHolder.getId())
        .name(accountHolder.getInfo().getName())
        .address(accountHolder.getResidencyAddress().getAddressLine1())
        .city(accountHolder.getResidencyAddress().getCity())
        .country(accountHolder.getResidencyAddress().getCountry())
        .postalCode(accountHolder.getResidencyAddress().getPostalCode())
        .build();
  }
}
