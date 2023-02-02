package app.bpartners.api.unit.validator;

import app.bpartners.api.endpoint.rest.model.Customer;
import app.bpartners.api.endpoint.rest.validator.CustomerRestValidator;
import org.junit.jupiter.api.Test;

import static app.bpartners.api.integration.conf.TestUtils.assertThrowsBadRequestException;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class CustomerRestValidatorTest {
  private final CustomerRestValidator validator = new CustomerRestValidator();

  @Test
  void validate_ok() {
    assertDoesNotThrow(
        () ->
            validator.accept(
                new Customer()
                    .id("some_id")
                    .firstName("first_name")
                    .lastName("last_name")
            )
    );
  }

  @Test
  void validate_ko() {
    assertThrowsBadRequestException(
        "Id is mandatory. firstName is mandatory. lastName is mandatory. ",
        () ->
            validator.accept(
                new Customer()
            ));
  }
}