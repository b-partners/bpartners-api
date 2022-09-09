package app.bpartners.api.unit.validator;

import static app.bpartners.api.integration.conf.TestUtils.assertThrowsBadRequestException;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import app.bpartners.api.endpoint.rest.model.CreatePreUser;
import app.bpartners.api.endpoint.rest.validator.PreUserRestValidator;
import org.junit.jupiter.api.Test;

public class PreUserRestValidatorTest {
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
    assertThrowsBadRequestException("email is missing. ",
        () -> preUserRestValidator.accept(
            new CreatePreUser()
                .email(null)
                .firstName("firstName_1")
                .lastName("lastName_1")
                .phone("phone1")
                .society("society1")
        )
    );
    assertThrowsBadRequestException("lastName is missing. society is missing. ",
        () -> preUserRestValidator.accept(
            new CreatePreUser()
                .email("email_id1")
                .firstName("firstName_1")
                .lastName(null)
                .phone("phone1")
                .society(null)
        )
    );
  }
}
