package app.bpartners.api.model.validator;

import app.bpartners.api.endpoint.rest.model.CreatePreRegistration;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class PreRegistrationValidator {
  private final String regex = "^(?=.{1,64}@)[A-Za-z0-9_-]+(\\\\.[A-Za-z0-9_-]+)"
          + "*@[^-][A-Za-z0-9-]+(\\\\.[A-Za-z0-9-]+)*(\\\\.[A-Za-z]{2,})$";

  public boolean accept(CreatePreRegistration createPreRegistration) {
    return Pattern.compile(regex)
            .matcher(createPreRegistration.getEmail())
            .matches();
  }
}
