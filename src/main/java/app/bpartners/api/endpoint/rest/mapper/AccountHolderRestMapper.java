package app.bpartners.api.endpoint.rest.mapper;


import app.bpartners.api.endpoint.rest.model.ContactAddress;
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
    ContactAddress contactAddress = new ContactAddress()
        .city(accountHolder.getCity())
        .address(accountHolder.getAddress())
        .postalCode(accountHolder.getPostalCode())
        .country(accountHolder.getCountry());
    restAccountHolder.setId(accountHolder.getId());
    restAccountHolder.setName(accountHolder.getName());
    restAccountHolder.setContactAddress(contactAddress);

    return restAccountHolder;
  }

  public AccountHolder toDomain(
      app.bpartners.api.endpoint.rest.model.AccountHolder restAccountHolder) {
    validator.accept(restAccountHolder);
    ContactAddress contactAddress = restAccountHolder.getContactAddress();
    return AccountHolder.builder()
        .id(restAccountHolder.getId())
        .name(restAccountHolder.getName())
        .city(contactAddress.getCity())
        .country(contactAddress.getCountry())
        .address(contactAddress.getAddress())
        .postalCode(contactAddress.getPostalCode())
        .build();
  }
}
