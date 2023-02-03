package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.CreateCustomer;
import app.bpartners.api.endpoint.rest.validator.CreateCustomerValidator;
import app.bpartners.api.endpoint.rest.validator.CustomerValidator;
import app.bpartners.api.model.Customer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class CustomerRestMapper {
  private final CreateCustomerValidator createCustomerValidator;
  private final CustomerValidator customerValidator;

  public app.bpartners.api.endpoint.rest.model.Customer toRest(Customer domain) {
    if (domain == null) {
      return null;
    }
    return new app.bpartners.api.endpoint.rest.model.Customer()
        .id(domain.getId())
        .name(domain.getName())
        .phone(domain.getPhone())
        .website(domain.getWebsite())
        .email(domain.getEmail())
        .address(domain.getAddress())
        .zipCode(domain.getZipCode())
        .city(domain.getCity())
        .country(domain.getCountry())
        .city(domain.getCity())
        .comment(domain.getComment());
  }

  public Customer toDomain(String accountId,
                           app.bpartners.api.endpoint.rest.model.Customer external) {
    customerValidator.accept(external);
    return Customer.builder()
        .id(external.getId())
        .idAccount(accountId)
        .name(external.getName())
        .phone(external.getPhone())
        .website(external.getWebsite())
        .email(external.getEmail())
        .address(external.getAddress())
        .zipCode(external.getZipCode())
        .city(external.getCity())
        .country(external.getCountry())
        .city(external.getCity())
        .comment(external.getComment())
        .build();
  }

  public Customer toDomain(String accountId, CreateCustomer rest) {
    createCustomerValidator.accept(rest);
    return Customer.builder()
        .id(null) //generated automatically
        .idAccount(accountId)
        .name(rest.getName())
        .phone(rest.getPhone())
        .website(rest.getWebsite())
        .email(rest.getEmail())
        .address(rest.getAddress())
        .zipCode(rest.getZipCode())
        .city(rest.getCity())
        .country(rest.getCountry())
        .comment(rest.getComment())
        .build();
  }
}
