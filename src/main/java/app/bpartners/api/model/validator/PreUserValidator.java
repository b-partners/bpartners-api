package app.bpartners.api.model.validator;

import static app.bpartners.api.service.utils.EmailUtils.isValidEmail;

import app.bpartners.api.model.PreUser;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class PreUserValidator {
  public void accept(PreUser preUser) {
    StringBuilder messageBuilder = new StringBuilder();
    if (!isValidEmail(preUser.getEmail())) {
      messageBuilder.append("Invalid email. ");
    }
    if (preUser.getMobilePhoneNumber() != null && !hasValidPhoneNumber(preUser)) {
      messageBuilder.append("Invalid phone number");
    }
    String errorMessage = messageBuilder.toString();
    if (!errorMessage.isEmpty()) {
      throw new BadRequestException(errorMessage);
    }
  }

  public void accept(List<PreUser> preUsers) {
    preUsers.forEach(this::accept);
  }

  private boolean hasValidPhoneNumber(PreUser preUser) {
    String emailPattern = "^(\\+\\d{1,3}( )?)?((\\(\\d{3}\\))|\\d{3})[- .]?\\d{3}[- .]?\\d{4}$";
    return Pattern.compile(emailPattern).matcher(preUser.getMobilePhoneNumber()).matches();
  }
}
