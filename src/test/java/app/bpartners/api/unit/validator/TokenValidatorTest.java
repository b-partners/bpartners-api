package app.bpartners.api.unit.validator;

import static app.bpartners.api.integration.conf.TestUtils.assertThrowsBadRequestException;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import app.bpartners.api.endpoint.rest.model.CreateToken;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.endpoint.rest.validator.TokenValidator;
import org.junit.jupiter.api.Test;

public class TokenValidatorTest {
  private final TokenValidator tokenValidator = new TokenValidator();

  @Test
  void validator_validate_token() {
    assertDoesNotThrow(() -> tokenValidator.accept(
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
  void validator_validate_invalid() {
    assertThrowsBadRequestException("code is missing. ",
        () -> tokenValidator.accept(
            new CreateToken()
                .redirectionStatusUrls(
                    new RedirectionStatusUrls()
                        .successUrl("success")
                        .failureUrl("failure")
                )
                .code(null)
        ));
    assertThrowsBadRequestException("redirectionStatusUrls is missing. ",
        () -> tokenValidator.accept(
            new CreateToken()
                .redirectionStatusUrls(
                    null
                )
                .code("code")
        ));
  }
}
