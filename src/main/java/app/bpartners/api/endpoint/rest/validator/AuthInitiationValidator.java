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
    if (authInitiation.getPhone() == null) {
      throw new BadRequestException("Phone is mandatory");
    }
    RedirectionStatusUrls statusUrls = authInitiation.getRedirectionStatusUrls();
    if (statusUrls == null) {
      throw new BadRequestException("RedirectionStatusUrls is mandatory");
    }
    if (statusUrls.getSuccessUrl() == null) {
      throw new BadRequestException("RedirectionStatusUrls.successUrl is mandatory");
    }
    if (statusUrls.getFailureUrl() == null) {
      throw new BadRequestException("RedirectionStatusUrls.failureUrl is mandatory");
    }
  }
}
