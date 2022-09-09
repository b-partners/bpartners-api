package app.bpartners.api.model.validator;

import app.bpartners.api.endpoint.rest.model.CreateToken;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class TokenValidator implements Consumer<CreateToken> {
  @Override
  public void accept(CreateToken createToken) {
    if (createToken.getCode() == null) {
      throw new BadRequestException("code is missing. ");
    }
    if (createToken.getRedirectionStatusUrls() == null) {
      throw new BadRequestException("redirectionStatusUrls is missing. ");
    }
  }
}
