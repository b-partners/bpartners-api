package app.bpartners.api.model.entity.validator;

import app.bpartners.api.endpoint.rest.model.AuthInitiation;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

//TODO: should not be in this package
@Component
public class AuthInitiationValidator implements Consumer<AuthInitiation> {
  @Override
  public void accept(AuthInitiation authInitiation) {
    if (authInitiation.getPhone() == null) {
      throw new BadRequestException("phone is mandatory");
    }
    RedirectionStatusUrls statusUrls = authInitiation.getRedirectionStatusUrls();
    if (statusUrls == null) {
      throw new BadRequestException("redirectionStatusUrls is mandatory");
    }
    if (statusUrls.getSuccessUrl() == null) {
      throw new BadRequestException("redirectionStatusUrls.successUrl is mandatory");
    }
    if (statusUrls.getFailureUrl() == null) {
      throw new BadRequestException("redirectionStatusUrls.failureUrl is mandatory");
    }
  }
}
