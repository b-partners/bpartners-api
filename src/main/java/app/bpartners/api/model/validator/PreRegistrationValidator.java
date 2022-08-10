package app.bpartners.api.model.validator;

import app.bpartners.api.model.PreRegistration;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class PreRegistrationValidator {
  private final String regex = "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\\\.[A-Za-z0-9_-]+)"
      + "*@[^-][A-Za-z0-9-]+(\\\\.[A-Za-z0-9-]+)*(\\\\.[A-Za-z]{2,})$";

  public boolean hasValidEmail(PreRegistration preRegistration) {
    return Pattern.compile(regex)
        .matcher(preRegistration.getEmail())
        .matches();
  }

  public void accept(PreRegistration preRegistration) {
    if (!hasValidEmail(preRegistration)) {
      throw new BadRequestException("Invalid email");
    }
  }
}
