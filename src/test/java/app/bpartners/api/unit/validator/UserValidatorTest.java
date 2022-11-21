package app.bpartners.api.unit.validator;

import app.bpartners.api.endpoint.rest.model.EnableStatus;
import app.bpartners.api.model.validator.UserValidator;
import app.bpartners.api.repository.jpa.model.HUser;
import org.junit.jupiter.api.Test;

import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_SWAN_USER_ID;
import static app.bpartners.api.integration.conf.TestUtils.USER1_ID;
import static app.bpartners.api.integration.conf.TestUtils.assertThrowsBadRequestException;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class UserValidatorTest {
  private final UserValidator userValidator = new UserValidator();

  @Test
  void valid_user() {
    assertDoesNotThrow(() -> userValidator.accept(
        HUser.builder()
            .id(USER1_ID)
            .logoFileId("logo.pdf")
            .swanUserId(JOE_DOE_SWAN_USER_ID)
            .status(EnableStatus.ENABLED)
            .monthlySubscription(5)
            .phoneNumber("+33611223344")
            .build()
    ));
  }

  @Test
  void invalid_user() {
    assertThrowsBadRequestException(
        "SwanUser identifier is mandatory. "
            + "Logo identifier is mandatory. "
            + "Phone number is mandatory. "
            + "Status is mandatory. ",
        () -> userValidator.accept(
            HUser.builder()
                .id(null)
                .swanUserId(null)
                .logoFileId(null)
                .status(null)
                .monthlySubscription(5)
                .phoneNumber(null)
                .build()
        ));
  }
}
