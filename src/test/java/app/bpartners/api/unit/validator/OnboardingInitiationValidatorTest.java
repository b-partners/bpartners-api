package app.bpartners.api.unit.validator;

import static app.bpartners.api.integration.conf.TestUtils.assertThrowsBadRequestException;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import app.bpartners.api.endpoint.rest.model.OnboardingInitiation;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.endpoint.rest.validator.OnboardingInitiationValidator;
import org.junit.jupiter.api.Test;

public class OnboardingInitiationValidatorTest {
  private final OnboardingInitiationValidator validator = new OnboardingInitiationValidator();

  @Test
  void validator_validate_onboardingInitiation_ok() {
    assertDoesNotThrow(() -> validator.accept(
        new OnboardingInitiation()
            .redirectionStatusUrls(
                new RedirectionStatusUrls()
                    .failureUrl("failure")
                    .successUrl("success")
            )
    ));
  }

  @Test
  void setValidator_validate_invalid_onboarding_ko() {
    assertThrowsBadRequestException("redirectionStatusUrls.successUrl is missing. ", () -> validator.accept(
        new OnboardingInitiation()
            .redirectionStatusUrls(
                new RedirectionStatusUrls()
                    .failureUrl("failure")
                    .successUrl(null)
            )
    ));
    assertThrowsBadRequestException("redirectionStatusUrls is missing. ", () -> validator.accept(
        new OnboardingInitiation()
            .redirectionStatusUrls(
                null
            )
    ));
  }
}
