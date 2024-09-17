package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.VisitorEmail;
import app.bpartners.api.model.exception.BadRequestException;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
public class VisitorEmailRestValidator implements Consumer<VisitorEmail> {
  @Override
  public void accept(VisitorEmail visitorEmail) {
    var errorMessage = new StringBuilder();
    if (visitorEmail.getEmail() == null) {
      errorMessage.append("Email is mandatory. ");
    }
    if (visitorEmail.getSubject() == null) {
      errorMessage.append("Subject is mandatory. ");
    }
    if (visitorEmail.getComments() == null) {
      errorMessage.append("Comments are mandatory.");
    }
    if (!errorMessage.isEmpty()) {
      throw new BadRequestException(errorMessage.toString());
    }
  }
}
