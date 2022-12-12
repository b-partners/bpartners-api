package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.Customer;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class CustomerRestValidator implements Consumer<Customer> {
  @Override
  public void accept(Customer customer) {
    StringBuilder exceptionMessageBuilder = new StringBuilder();
    if (customer.getId() == null) {
      exceptionMessageBuilder.append("Id is mandatory. ");
    }
    if (customer.getName() == null) {
      exceptionMessageBuilder.append("Name is mandatory. ");
    }
    String exceptionMessage = exceptionMessageBuilder.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }
}
