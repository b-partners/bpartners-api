package app.bpartners.api.unit.validator;

import static app.bpartners.api.integration.conf.TestUtils.assertThrowsBadRequestException;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import app.bpartners.api.endpoint.rest.model.AuthInitiation;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.endpoint.rest.validator.AuthInitiationValidator;
import org.junit.jupiter.api.Test;

public class AuthInitiationValidatorTest {
  private final AuthInitiationValidator authInitiationValidator = new AuthInitiationValidator();

  @Test
  void validator_validate_authInitiation_ok() {
    assertDoesNotThrow(
        () -> authInitiationValidator.accept(
            new AuthInitiation()
                .phone("phone")
                .redirectionStatusUrls(
                    new RedirectionStatusUrls()
                        .successUrl("success")
                        .failureUrl("failure")
                )
                .state("1234")
        )
    );
  }

  @Test
  void validator_validate_invalid_authInitiation_ko() {
    assertThrowsBadRequestException("redirectionStatusUrls is missing. ",
        () -> authInitiationValidator.accept(
            new AuthInitiation()
                .phone("phone")
                .redirectionStatusUrls(
                    null
                )
                .state("1234")
        )
    );
    assertThrowsBadRequestException("phone is missing. ",
        () -> authInitiationValidator.accept(
            new AuthInitiation()
                .phone(null)
                .redirectionStatusUrls(
                    new RedirectionStatusUrls()
                        .successUrl("success")
                        .failureUrl("failure")
                )
                .state("1234")
        )
    );
  }
}
