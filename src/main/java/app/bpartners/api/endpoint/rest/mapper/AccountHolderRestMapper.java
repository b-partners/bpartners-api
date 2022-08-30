package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.model.AccountHolder;
import org.springframework.stereotype.Component;

@Component
public class AccountHolderRestMapper {
  public app.bpartners.api.endpoint.rest.model.AccountHolder toRest(AccountHolder accountHolder) {
    app.bpartners.api.endpoint.rest.model.AccountHolder restAccountHolder =
        new app.bpartners.api.endpoint.rest.model.AccountHolder();
    restAccountHolder.setId(accountHolder.getId());
    restAccountHolder.setName(accountHolder.getName());
    restAccountHolder.setCity(accountHolder.getCity());
    restAccountHolder.setAddress(accountHolder.getAddress());
    restAccountHolder.postalCode(accountHolder.getPostalCode());
    restAccountHolder.setCountry(accountHolder.getCountry());

    return restAccountHolder;
  }

  public AccountHolder toDomain(
      app.bpartners.api.endpoint.rest.model.AccountHolder restAccountHolder) {
    return AccountHolder.builder()
        .id(restAccountHolder.getId())
        .name(restAccountHolder.getName())
        .city(restAccountHolder.getCity())
        .country(restAccountHolder.getCountry())
        .address(restAccountHolder.getAddress())
        .postalCode(restAccountHolder.getPostalCode())
        .build();
  }
}
