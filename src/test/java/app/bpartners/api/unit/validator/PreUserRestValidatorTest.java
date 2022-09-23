package app.bpartners.api.unit.validator;

import app.bpartners.api.endpoint.rest.model.CreatePreUser;
import app.bpartners.api.endpoint.rest.validator.PreUserRestValidator;
import org.junit.jupiter.api.Test;
import static app.bpartners.api.integration.conf.TestUtils.assertThrowsBadRequestException;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class PreUserRestValidatorTest {
  PreUserRestValidator preUserRestValidator = new PreUserRestValidator();

  @Test
  void validator_validate_createPreUser_ok() {
    assertDoesNotThrow(() -> preUserRestValidator.accept(
            new CreatePreUser()
                .email("email_id1")
                .firstName("firstName_1")
                .lastName("lastName_1")
                .phone("phone1")
                .society("society1")
        )
    );
  }

  @Test
  void validator_validate_createPreUser_ko() {
    assertThrowsBadRequestException(
        "firstName is missing. "
            + "lastName is missing. "
            + "society is missing. "
            + "email is missing. "
            + "mobilePhoneNumber is missing. ",
        () -> preUserRestValidator.accept(
            new CreatePreUser()
                .email(null)
                .firstName(null)
                .lastName(null)
                .phone(null)
                .society(null)
        )
    );
  }
}
