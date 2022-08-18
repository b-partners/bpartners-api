package app.bpartners.api.model.entity.validator;

import app.bpartners.api.model.entity.HPreUser;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class PreRegistrationValidator {
  private final String regex =
      "(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])";

  public boolean hasValidEmail(HPreUser HPreUser) {
    return Pattern.compile(regex)
        .matcher(HPreUser.getEmail())
        .matches();
  }

  public void accept(HPreUser HPreUser) {
    if (HPreUser.getEmail() == null) {
      throw new BadRequestException("Email is mandatory");
    }
    if (!hasValidEmail(HPreUser)) {
      throw new BadRequestException("Invalid email");
    }
  }

  public void accept(List<HPreUser> HPreUsers) {
    HPreUsers.forEach(this::accept);
  }
}
