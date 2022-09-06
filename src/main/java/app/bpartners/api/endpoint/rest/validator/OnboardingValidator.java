package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.OnboardingInitiation;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.model.exception.BadRequestException;
import org.springframework.stereotype.Component;

@Component
public class OnboardingValidator {

  public void accept(OnboardingInitiation initiation) {
    if (initiation.getRedirectionStatusUrls() == null) {
      throw new BadRequestException("Should not be null");
    }
    RedirectionStatusUrls statusUrls = initiation.getRedirectionStatusUrls();

    if (statusUrls.getSuccessUrl() == null) {
      throw new BadRequestException("successUrl is mandatory");
    }
    if (statusUrls.getFailureUrl() == null) {
      throw new BadRequestException("failureUrl is mandatory");
    }
  }

}
