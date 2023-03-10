package app.bpartners.api.unit.validator;

import app.bpartners.api.endpoint.rest.model.CreateCustomer;
import app.bpartners.api.endpoint.rest.validator.CreateCustomerValidator;
import org.junit.jupiter.api.Test;

import static app.bpartners.api.integration.conf.TestUtils.assertThrowsBadRequestException;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class CustomerValidatorTest {
  private final CreateCustomerValidator validator = new CreateCustomerValidator();

  @Test
  void validator_validate_valid_create_customer_ok() {
    assertDoesNotThrow(() ->
        validator.accept(
            new CreateCustomer()
                .address("address")
                .city("city")
                .firstName("firstname")
                .lastName("lastname")
                .country("country1")
                .email("email1")
                .phone("phone1")
                .zipCode(1)
                .website("website")
        )
    );
  }

  @Test
  void validator_validate_invalid_create_customer_ko() {
    assertThrowsBadRequestException(
        "firstName is mandatory. lastName is mandatory. "
        ,
        () ->
            validator.accept(
                new CreateCustomer()
                    .firstName(null)
                    .lastName(null)
                    .address("address")
                    .city("city")
                    .country("country1")
                    .email("email1")
                    .phone("phone1")
                    .zipCode(1)
                    .website("website")
            )
    );
    assertThrowsBadRequestException(
        "firstName is mandatory. "
            + "lastName is mandatory. "
            + "Email is mandatory. "
            + "Phone is mandatory. "
            + "Address is mandatory. "
        ,
        () ->
            validator.accept(
                new CreateCustomer()
                    .address(null)
                    .city(null)
                    .firstName(null)
                    .lastName(null)
                    .country(null)
                    .email(null)
                    .phone(null)
                    .zipCode(null)
                    .website(null)
            )
    );
  }
}
