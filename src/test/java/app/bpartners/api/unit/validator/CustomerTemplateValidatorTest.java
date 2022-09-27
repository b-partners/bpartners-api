package app.bpartners.api.unit.validator;

import app.bpartners.api.endpoint.rest.model.CreateCustomer;
import app.bpartners.api.endpoint.rest.validator.CustomerValidator;
import org.junit.jupiter.api.Test;
import static app.bpartners.api.integration.conf.TestUtils.assertThrowsBadRequestException;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class CustomerTemplateValidatorTest {
  private final CustomerValidator customerValidator = new CustomerValidator();

  @Test
  void validator_validate_customer_ok() {
    assertDoesNotThrow(() -> customerValidator.accept(
        new CreateCustomer()
            .phone("phone")
            .address("address")
            .email("email")
            .name("name")
    ));
  }

  @Test
  void validator_validate_invalid_customer_ko() {
    assertThrowsBadRequestException("address is missing. "
            + "name is missing. "
            + "phone is missing. "
            + "email is missing. ",
        () -> customerValidator.accept(
            new CreateCustomer()
                .phone(null)
                .address(null)
                .email(null)
                .name(null)
        ));
  }
}
