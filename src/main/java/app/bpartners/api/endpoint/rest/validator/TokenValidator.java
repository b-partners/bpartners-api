package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.CreateToken;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.model.exception.BadRequestException;

public class TokenValidator {
  public void accept(CreateToken createToken) {
    StringBuilder exceptionMessageBuilder = new StringBuilder();
    if (createToken.getCode() == null) {
      exceptionMessageBuilder.append("code is missing. ");
    }
    if (createToken.getRedirectionStatusUrls() == null) {
      exceptionMessageBuilder.append("redirectionStatusUrls is missing. ");
    }
    if (createToken.getRedirectionStatusUrls() != null) {
      RedirectionStatusUrls statusUrls = createToken.getRedirectionStatusUrls();
      if (statusUrls.getFailureUrl() == null) {
        exceptionMessageBuilder.append("redirectionStatusUrls.failureUrl");
      }
      if (statusUrls.getSuccessUrl() == null) {
        exceptionMessageBuilder.append("redirectionStatusUrls.successUrl");
      }
    }
    String exceptionMessage = exceptionMessageBuilder.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }
}
