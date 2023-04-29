package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.CreateCustomer;
import app.bpartners.api.endpoint.rest.model.Customer;
import app.bpartners.api.endpoint.rest.model.CustomerStatus;
import app.bpartners.api.endpoint.rest.validator.CreateCustomerValidator;
import app.bpartners.api.endpoint.rest.validator.CustomerValidator;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class CustomerRestMapper {
  public static final String EMPTY_SPACE = " ";
  private final CreateCustomerValidator createCustomerValidator;
  private final CustomerValidator customerValidator;

  public Customer toRest(app.bpartners.api.model.Customer domain) {
    if (domain == null) {
      return null;
    }

    return new Customer()
        .id(domain.getId())
        .firstName(domain.getFirstName())
        .lastName(domain.getLastName())
        .phone(domain.getPhone())
        .website(domain.getWebsite())
        .email(domain.getEmail())
        .address(domain.getAddress())
        .zipCode(domain.getZipCode())
        .city(domain.getCity())
        .country(domain.getCountry())
        .city(domain.getCity())
        .comment(domain.getComment())
        .status(domain.getStatus());
  }

  public app.bpartners.api.model.Customer toDomain(String accountId, Customer rest) {
    customerValidator.accept(rest);
    String[] names =
        retrieveNames(rest.getFirstName(), rest.getLastName());
    String firstName = names.length != 0 ? names[0] : null;
    String lastName =
        names.length == 0 || (names.length == 2
            && names[1] != null
            && (names[1].equals(EMPTY_SPACE) || names[1].equals("")))
            ? null
            : names[1];
    return app.bpartners.api.model.Customer.builder()
        .id(rest.getId())
        .idAccount(accountId)
        .firstName(firstName)
        .lastName(lastName)
        .phone(rest.getPhone())
        .website(rest.getWebsite())
        .email(rest.getEmail())
        .address(rest.getAddress())
        .zipCode(rest.getZipCode())
        .city(rest.getCity())
        .country(rest.getCountry())
        .city(rest.getCity())
        .comment(rest.getComment())
        .status(rest.getStatus())
        .build();
  }

  public app.bpartners.api.model.Customer toDomain(String accountId, CreateCustomer rest) {
    createCustomerValidator.accept(rest);
    String[] names = retrieveNames(rest.getFirstName(), rest.getLastName());
    String firstName = names.length != 0 ? names[0] : null;
    String lastName =
        names.length == 0
            || (names.length == 2 && names[1] != null
            && (names[1].isEmpty() || names[1].isBlank())) ? null
            : names[1];
    return app.bpartners.api.model.Customer.builder()
        .id(null) //generated automatically
        .idAccount(accountId)
        .firstName(firstName)
        .lastName(lastName)
        .phone(rest.getPhone())
        .website(rest.getWebsite())
        .email(rest.getEmail())
        .address(rest.getAddress())
        .zipCode(rest.getZipCode())
        .city(rest.getCity())
        .country(rest.getCountry())
        .comment(rest.getComment())
        .status(CustomerStatus.ENABLED) // set to enabled by default
        .build();
  }

  private String[] retrieveNames(String firstName, String lastName) {
    if (firstName != null || lastName != null) {
      return new String[] {firstName, lastName};
    }
    return new String[0];
  }
}
