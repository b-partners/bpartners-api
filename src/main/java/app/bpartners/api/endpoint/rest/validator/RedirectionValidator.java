package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.model.exception.BadRequestException;

public class RedirectionValidator {
  private RedirectionValidator() {

  }

  public static void verifyRedirectionStatusUrls(StringBuilder exceptionMessageBuilder,
                                                 RedirectionStatusUrls statusUrls) {
    if (statusUrls == null) {
      exceptionMessageBuilder.append("redirectionStatusUrls is mandatory. ");
    }
    if (statusUrls != null) {
      if (statusUrls.getSuccessUrl() == null) {
        exceptionMessageBuilder.append("redirectionStatusUrls.successUrl is mandatory. ");
      }
      if (statusUrls.getFailureUrl() == null) {
        exceptionMessageBuilder.append("redirectionStatusUrls.failureUrl is mandatory. ");
      }
    }
    String exceptionMessage = exceptionMessageBuilder.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }
}
