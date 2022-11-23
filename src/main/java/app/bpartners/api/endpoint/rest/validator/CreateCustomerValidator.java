package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.CreateCustomer;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class CreateCustomerValidator implements Consumer<CreateCustomer> {

  @Override
  public void accept(CreateCustomer createCustomer) {
    StringBuilder exceptionMessageBuilder = new StringBuilder();
    if (createCustomer.getName() == null) {
      exceptionMessageBuilder.append("Name is mandatory. ");
    }
    if (createCustomer.getEmail() == null) {
      exceptionMessageBuilder.append("Email is mandatory. ");
    }
    if (createCustomer.getPhone() == null) {
      exceptionMessageBuilder.append("Phone is mandatory. ");
    }
    if (createCustomer.getAddress() == null) {
      exceptionMessageBuilder.append("Address is mandatory. ");
    }
    String exceptionMessage = exceptionMessageBuilder.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }
}
