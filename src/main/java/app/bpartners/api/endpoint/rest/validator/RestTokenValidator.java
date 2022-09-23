package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.CreateToken;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class RestTokenValidator implements Consumer<CreateToken> {
  @Override
  public void accept(CreateToken createToken) {
    StringBuilder exceptionMessageBuilder = new StringBuilder();
    if (createToken.getCode() == null) {
      exceptionMessageBuilder.append("code is mandatory. ");
    }
    OnboardingInitiationValidator.verifyRedirectionStatusUrls(exceptionMessageBuilder,
        createToken.getRedirectionStatusUrls());
    String exceptionMessage = exceptionMessageBuilder.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }
}
