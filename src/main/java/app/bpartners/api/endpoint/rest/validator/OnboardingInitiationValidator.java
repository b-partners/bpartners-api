package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.OnboardingInitiation;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.model.exception.BadRequestException;
import org.springframework.stereotype.Component;

@Component
public class OnboardingInitiationValidator {

  public static void verifyRedirectionStatusUrls(StringBuilder exceptionMessageBuilder,
                                                 RedirectionStatusUrls statusUrls) {
    if (statusUrls == null) {
      exceptionMessageBuilder.append("redirectionStatusUrls is missing. ");
    }
    if (statusUrls != null) {
      if (statusUrls.getSuccessUrl() == null) {
        exceptionMessageBuilder.append("redirectionStatusUrls.successUrl is missing. ");
      }
      if (statusUrls.getFailureUrl() == null) {
        exceptionMessageBuilder.append("redirectionStatusUrls.failureUrl is missing. ");
      }
    }
    String exceptionMessage = exceptionMessageBuilder.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }

  public void accept(OnboardingInitiation initiation) {
    StringBuilder exceptionMessageBuilder = new StringBuilder();
    verifyRedirectionStatusUrls(exceptionMessageBuilder, initiation.getRedirectionStatusUrls());
  }

}
