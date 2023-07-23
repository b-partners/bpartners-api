package app.bpartners.api.unit.validator;

import app.bpartners.api.endpoint.rest.model.CreateToken;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.endpoint.rest.validator.RestTokenValidator;
import org.junit.jupiter.api.Test;
import static app.bpartners.api.integration.conf.utils.TestUtils.assertThrowsBadRequestException;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class RestTokenValidatorTest {
  private final RestTokenValidator restTokenValidator = new RestTokenValidator();

  @Test
  void validator_validate_token_ok() {
    assertDoesNotThrow(() -> restTokenValidator.accept(
        new CreateToken()
            .redirectionStatusUrls(
                new RedirectionStatusUrls()
                    .successUrl("success")
                    .failureUrl("failure")
            )
            .code("code")
    ));
  }

  @Test
  void validator_validate_invalid_token_ko() {
    assertThrowsBadRequestException(
        "code is mandatory. "
            + "redirectionStatusUrls.successUrl is mandatory. "
            + "redirectionStatusUrls.failureUrl is mandatory. ",
        () -> restTokenValidator.accept(
            new CreateToken()
                .redirectionStatusUrls(
                    new RedirectionStatusUrls()
                        .successUrl(null)
                        .failureUrl(null)
                )
                .code(null)
        ));
    assertThrowsBadRequestException("redirectionStatusUrls is mandatory. ",
        () -> restTokenValidator.accept(
            new CreateToken()
                .redirectionStatusUrls(
                    null
                )
                .code("code")
        ));
  }
}
