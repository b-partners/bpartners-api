package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.AuthInitiation;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

import static app.bpartners.api.endpoint.rest.validator.RedirectionValidator.verifyRedirectionStatusUrls;

@Component
public class AuthInitiationValidator implements Consumer<AuthInitiation> {
  @Override
  public void accept(AuthInitiation authInitiation) {
    StringBuilder exceptionMessageBuilder = new StringBuilder();
    if (authInitiation.getPhone() == null) {
      exceptionMessageBuilder.append("phone is mandatory. ");
    }
    if (authInitiation.getState() == null) {
      exceptionMessageBuilder.append("state is mandatory. ");
    }
    verifyRedirectionStatusUrls(exceptionMessageBuilder, authInitiation.getRedirectionStatusUrls());
  }
}
