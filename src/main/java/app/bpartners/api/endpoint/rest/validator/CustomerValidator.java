package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.Customer;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class CustomerValidator implements Consumer<Customer> {

  @Override
  public void accept(Customer toUpdateCustomer) {
    StringBuilder exceptionMessageBuilder = new StringBuilder();
    if (toUpdateCustomer.getId() == null) {
      exceptionMessageBuilder.append("Identifier must not be null. ");
    }
    if (toUpdateCustomer.getFirstName() == null) {
      exceptionMessageBuilder.append("firstName not be null.");
    }
    if (toUpdateCustomer.getLastName() == null) {
      exceptionMessageBuilder.append("lastName not be null.");
    }
    String exceptionMessage = exceptionMessageBuilder.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }
}
