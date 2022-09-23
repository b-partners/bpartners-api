package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.CreateCustomer;
import app.bpartners.api.endpoint.rest.model.Customer;
import app.bpartners.api.endpoint.rest.validator.CreateCustomerValidator;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class CustomerRestMapper {
  private final CreateCustomerValidator validator;

  public Customer toRest(app.bpartners.api.model.Customer domain) {
    return new Customer()
        .id(domain.getId())
        .name(domain.getName())
        .phone(domain.getPhone())
        .website(domain.getWebsite())
        .email(domain.getEmail())
        .address(domain.getAddress())
        .zipCode(domain.getZipCode())
        .city(domain.getCity())
        .country(domain.getCountry())
        .city(domain.getCity());
  }

  public app.bpartners.api.model.Customer toDomain(String accountId, CreateCustomer rest) {
    validator.accept(rest);
    return app.bpartners.api.model.Customer.builder()
        .id(null)
        .idAccount(accountId)
        .name(rest.getName())
        .phone(rest.getPhone())
        .website(rest.getWebsite())
        .email(rest.getEmail())
        .address(rest.getAddress())
        .zipCode(rest.getZipCode())
        .city(rest.getCity())
        .country(rest.getCountry())
        .build();
  }

  public app.bpartners.api.model.Customer toDomain(String accountId, Customer rest) {
    return app.bpartners.api.model.Customer.builder()
        .id(rest.getId())
        .idAccount(accountId)
        .name(rest.getName())
        .phone(rest.getPhone())
        .website(rest.getWebsite())
        .email(rest.getEmail())
        .address(rest.getAddress())
        .zipCode(rest.getZipCode())
        .city(rest.getCity())
        .country(rest.getCountry())
        .build();
  }
}
