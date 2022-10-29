package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.CustomEmail;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class CustomEmailValidator implements Consumer<CustomEmail> {

  @Override
  public void accept(CustomEmail customEmail) {
    StringBuilder exceptionMessageBuilder = new StringBuilder();
    if (customEmail.getMessage() == null) {
      exceptionMessageBuilder.append("Message is mandatory. ");
    }
    if (customEmail.getSubject() == null) {
      exceptionMessageBuilder.append("Subject is mandatory. ");
    }
    String exceptionMessage = exceptionMessageBuilder.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }
}
