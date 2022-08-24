package app.bpartners.api.model.validator;

import app.bpartners.api.model.PreUser;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class PreUserValidator {
  public void accept(PreUser preUser) {
    if (!hasValidEmail(preUser)) {
      throw new BadRequestException("Invalid email");
    }
  }

  public void accept(List<PreUser> preUsers) {
    preUsers.forEach(this::accept);
  }

  private boolean hasValidEmail(PreUser preUser) {
    String pattern = "[A-Za-z0-9_-]";
    String emailPattern = "^(?=.{1,64}@)" + pattern + "+(\\." + pattern + "+)*@"
        + "[^-]" + pattern + "+(\\." + pattern + "+)*(\\.[A-Za-z]{2,})$";
    return Pattern.compile(emailPattern)
      .matcher(preUser.getEmail())
      .matches();
  }
}
