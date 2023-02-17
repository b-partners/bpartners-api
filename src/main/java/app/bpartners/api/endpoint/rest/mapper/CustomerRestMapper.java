package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.CreateCustomer;
import app.bpartners.api.endpoint.rest.validator.CreateCustomerValidator;
import app.bpartners.api.endpoint.rest.validator.CustomerValidator;
import app.bpartners.api.model.Customer;
import java.util.Arrays;
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

  public app.bpartners.api.endpoint.rest.model.Customer toRest(Customer domain) {
    if (domain == null) {
      return null;
    }
    String name = domain.getFirstName() + " " + domain.getLastName();
    if (domain.getFirstName() == null) {
      name = domain.getLastName();
    } else if (domain.getLastName() == null) {
      name = domain.getFirstName();
    }
    return new app.bpartners.api.endpoint.rest.model.Customer()
        .id(domain.getId())
        .name(name)
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
        .comment(domain.getComment());
  }

  public Customer toDomain(String accountId,
                           app.bpartners.api.endpoint.rest.model.Customer external) {
    customerValidator.accept(external);
    String[] names =
        retrieveNames(external.getName(), external.getFirstName(), external.getLastName());
    String firstName = names.length != 0 ? names[0] : null;
    String lastName =
        names.length == 0 || (names.length == 2
            && names[1] != null
            && (names[1].equals(EMPTY_SPACE) || names[1].equals("")))
            ? null
            : names[1];
    return Customer.builder()
        .id(external.getId())
        .idAccount(accountId)
        .firstName(firstName)
        .lastName(lastName)
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
    String[] names = retrieveNames(rest.getName(), rest.getFirstName(), rest.getLastName());
    String firstName = names.length != 0 ? names[0] : null;
    String lastName =
        names.length == 0
            || (names.length == 2 && names[1] != null
            && (names[1].isEmpty() || names[1].isBlank())) ? null
            : names[1];
    return Customer.builder()
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
        .build();
  }

  public Customer toDomainWithoutCheck(String accountId, CreateCustomer toCreate) {
    return Customer.builder()
        .idAccount(accountId)
        .firstName(toCreate.getFirstName())
        .lastName(toCreate.getLastName())
        .email(toCreate.getEmail())
        .address(toCreate.getAddress())
        .phone(toCreate.getPhone())
        .website(toCreate.getWebsite())
        .city(toCreate.getCity())
        .country(toCreate.getCountry())
        .comment(toCreate.getComment())
        .build();
  }

  private String[] retrieveNames(String name, String firstName, String lastName) {
    if (firstName != null || lastName != null) {
      return new String[] {firstName, lastName};
    }
    if (name != null) {
      log.warn("DEPRECATED : Customer.name is deprecated. "
          + "Use Customer.firstName and Customer.lastName instead");
      String[] names = name.split(EMPTY_SPACE);
      return new String[] {
          names[0], String.join(" ",
          Arrays.asList(names).subList(1, names.length))};
    }
    return new String[0];
  }
}
