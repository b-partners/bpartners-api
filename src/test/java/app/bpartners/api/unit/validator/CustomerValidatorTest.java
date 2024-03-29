package app.bpartners.api.unit.validator;

import static app.bpartners.api.integration.conf.utils.TestUtils.assertThrowsBadRequestException;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import app.bpartners.api.endpoint.rest.model.CreateCustomer;
import app.bpartners.api.endpoint.rest.validator.CreateCustomerValidator;
import org.junit.jupiter.api.Test;

class CustomerValidatorTest {
  private final CreateCustomerValidator validator = new CreateCustomerValidator();

  @Test
  void validator_validate_valid_create_customer_ok() {
    assertDoesNotThrow(
        () ->
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
                    .website("website")));
  }

  @Test
  void validator_validate_invalid_create_customer_ko() {
    assertThrowsBadRequestException(
        "Either firstName or lastName is mandatory. ",
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
                    .website("website")));
    assertThrowsBadRequestException(
        "Either firstName or lastName is mandatory. "
            + "Either email or phone is mandatory. "
            + "Address is mandatory. ",
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
                    .website(null)));
  }
}
