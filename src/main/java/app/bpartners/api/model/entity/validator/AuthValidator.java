package app.bpartners.api.model.entity.validator;

import app.bpartners.api.endpoint.rest.model.AuthParams;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class AuthValidator implements Consumer<AuthParams> {
  @Override
  public void accept(AuthParams params) {
    if (params.getPhoneNumber() == null) {
      throw new BadRequestException("Phone number is mandatory");
    }
    if (params.getSuccessUrl() == null) {
      throw new BadRequestException("Success URL is mandatory");
    }
    if (params.getFailureUrl() == null) {
      throw new BadRequestException("Failure URL is mandatory");
    }
  }
}
