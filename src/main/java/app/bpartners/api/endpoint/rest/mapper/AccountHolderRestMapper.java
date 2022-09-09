package app.bpartners.api.endpoint.rest.mapper;


import app.bpartners.api.endpoint.rest.validator.AccountHolderValidator;
import app.bpartners.api.model.AccountHolder;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AccountHolderRestMapper {

  private final AccountHolderValidator validator;

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
    validator.accept(restAccountHolder);
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
