package app.bpartners.api.endpoint.rest.validator;


import app.bpartners.api.endpoint.rest.model.CreatePreUser;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class PreUserRestValidator implements Consumer<CreatePreUser> {
  @Override
  public void accept(CreatePreUser createPreUser) {
    StringBuilder exceptionMessageBuilder = new StringBuilder();
    if (createPreUser.getFirstName() == null) {
      exceptionMessageBuilder.append("firstName is missing. ");
    }
    if (createPreUser.getLastName() == null) {
      exceptionMessageBuilder.append("lastName is missing. ");
    }
    if (createPreUser.getSociety() == null) {
      exceptionMessageBuilder.append("society is missing. ");
    }
    if (createPreUser.getEmail() == null) {
      exceptionMessageBuilder.append("email is missing. ");
    }
    if (createPreUser.getPhone() == null) {
      exceptionMessageBuilder.append("mobilePhoneNumber is missing. ");
    }
    String exceptionMessage = exceptionMessageBuilder.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }
}
