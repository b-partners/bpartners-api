package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.AuthInitiation;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class AuthInitiationValidator implements Consumer<AuthInitiation> {
  @Override
  public void accept(AuthInitiation authInitiation) {
    StringBuilder exceptionMessageBuilder = new StringBuilder();
    if (authInitiation.getPhone() == null) {
      exceptionMessageBuilder.append("phone is missing. ");
    }
    RedirectionStatusUrls statusUrls = authInitiation.getRedirectionStatusUrls();
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
}
