package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.CreateCustomer;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class CustomerValidator implements Consumer<CreateCustomer> {
  @Override
  public void accept(CreateCustomer rest) {
    StringBuilder exceptionMessageBuilder = new StringBuilder();
    if (rest.getAddress() == null) {
      exceptionMessageBuilder.append("address is missing. ");
    }
    if (rest.getName() == null) {
      exceptionMessageBuilder.append("name is missing. ");
    }
    if (rest.getPhone() == null) {
      exceptionMessageBuilder.append("phone is missing. ");
    }
    if (rest.getEmail() == null) {
      exceptionMessageBuilder.append("email is missing. ");
    }
    String exceptionMessage = exceptionMessageBuilder.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }
}
