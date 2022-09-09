package app.bpartners.api.unit.validator;

import app.bpartners.api.endpoint.rest.model.AuthInitiation;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.endpoint.rest.validator.AuthInitiationValidator;
import org.junit.jupiter.api.Test;
import static app.bpartners.api.integration.conf.TestUtils.assertThrowsBadRequestException;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class AuthInitiationValidatorTest {
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
    assertThrowsBadRequestException("state is missing. redirectionStatusUrls is missing. ",
        () -> authInitiationValidator.accept(
            new AuthInitiation()
                .phone("phone")
                .redirectionStatusUrls(
                    null
                )
                .state(null)
        )
    );
    assertThrowsBadRequestException("phone is missing. "
            + "redirectionStatusUrls.successUrl is missing. "
            + "redirectionStatusUrls.failureUrl is missing. ",
        () -> authInitiationValidator.accept(
            new AuthInitiation()
                .phone(null)
                .redirectionStatusUrls(
                    new RedirectionStatusUrls()
                        .successUrl(null)
                        .failureUrl(null)
                )
                .state("1234")
        )
    );
  }
}
