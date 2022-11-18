package app.bpartners.api.model.validator;

import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.repository.jpa.model.HUser;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class UserValidator implements Consumer<HUser> {

  @Override
  public void accept(HUser user) {
    StringBuilder message = new StringBuilder();
    if (user.getSwanUserId() == null) {
      message.append("SwanUser identifier is mandatory. ");
    }
    if (user.getLogoFileId() == null) {
      message.append("Logo identifier is mandatory. ");
    }
    if (user.getPhoneNumber() == null) {
      message.append("Phone number is mandatory. ");
    }
    if (user.getStatus() == null) {
      message.append("Status is mandatory. ");
    }
    String exceptionMessage = message.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }
}
