package app.bpartners.api.endpoint.rest.mapper;

import static app.bpartners.api.endpoint.rest.model.CustomerStatus.ENABLED;
import static java.util.UUID.randomUUID;

import app.bpartners.api.endpoint.rest.model.CreateCustomer;
import app.bpartners.api.endpoint.rest.model.Customer;
import app.bpartners.api.endpoint.rest.model.CustomerLocation;
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
    CustomerLocation customerLocation =
        new CustomerLocation()
            .address(domain.getAddress())
            // TODO: use getCoordinate instead
            .latitude(domain.getLocation().getLatitude())
            .longitude(domain.getLocation().getLongitude());
    return new Customer()
        .id(domain.getId())
        .name(domain.getRealName())
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
        .location(customerLocation)
        .status(domain.getStatus())
        .updatedAt(domain.getUpdatedAt())
        .customerType(domain.getCustomerType())
        .isConverted(domain.isConverted())
        .createdAt(domain.getCreatedAt());
  }

  public app.bpartners.api.model.Customer toDomain(String idUser, Customer rest) {
    customerValidator.accept(rest);
    String[] names = retrieveNames(rest.getFirstName(), rest.getLastName());
    String firstName = names.length != 0 ? names[0] : null;
    String lastName =
        names.length == 0
                || (names.length == 2
                    && names[1] != null
                    && (names[1].equals(EMPTY_SPACE) || names[1].equals("")))
            ? null
            : names[1];
    app.bpartners.api.model.Location customerLocation = null;
    if (rest.getLocation() == null) {
      customerLocation =
          app.bpartners.api.model.Location.builder().address(rest.getAddress()).build();
    } else {
      customerLocation =
          app.bpartners.api.model.Location.builder()
              .address(rest.getAddress())
              .latitude(
                  rest.getLocation().getLatitude() == null
                      ? null
                      : rest.getLocation().getLatitude())
              .longitude(
                  rest.getLocation().getLongitude() == null
                      ? null
                      : rest.getLocation().getLongitude())
              .build();
    }
    String name = null;
    if (rest.getName() != null) {
      name = rest.getName();
    } else {
      if (rest.getFirstName() != null) {
        name = rest.getFirstName();
      }
      if (rest.getLastName() != null) {
        name += " " + rest.getLastName();
      }
    }
    return app.bpartners.api.model.Customer.builder()
        .id(rest.getId())
        .idUser(idUser)
        .name(name)
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
        .location(customerLocation)
        .status(rest.getStatus())
        .customerType(rest.getCustomerType())
        .isConverted(rest.getIsConverted() == null || rest.getIsConverted())
        .build();
  }

  public app.bpartners.api.model.Customer toDomain(String userId, CreateCustomer rest) {
    createCustomerValidator.accept(rest);
    String[] names = retrieveNames(rest.getFirstName(), rest.getLastName());
    String firstName = names.length != 0 ? names[0] : null;
    String lastName =
        names.length == 0
                || (names.length == 2
                    && names[1] != null
                    && (names[1].isEmpty() || names[1].isBlank()))
            ? null
            : names[1];
    String name = null;
    if (rest.getName() != null) {
      name = rest.getName();
    } else {
      if (rest.getFirstName() != null) {
        name = rest.getFirstName();
      }
      if (rest.getLastName() != null) {
        name += " " + rest.getLastName();
      }
    }
    return app.bpartners.api.model.Customer.builder()
        .id(String.valueOf(randomUUID()))
        .idUser(userId)
        .firstName(firstName)
        .lastName(lastName)
        .name(name)
        .phone(rest.getPhone())
        .website(rest.getWebsite())
        .email(rest.getEmail())
        .address(rest.getAddress())
        .zipCode(rest.getZipCode())
        .city(rest.getCity())
        .country(rest.getCountry())
        .comment(rest.getComment())
        .location(app.bpartners.api.model.Location.builder().address(rest.getAddress()).build())
        .status(ENABLED) // set to enabled by default
        .customerType(rest.getCustomerType())
        .isConverted(true) // set to true by default
        .build();
  }

  private String[] retrieveNames(String firstName, String lastName) {
    if (firstName != null || lastName != null) {
      return new String[] {firstName, lastName};
    }
    return new String[0];
  }
}
