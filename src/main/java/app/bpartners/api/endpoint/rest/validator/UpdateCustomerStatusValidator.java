package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.UpdateCustomerStatus;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class UpdateCustomerStatusValidator implements Consumer<UpdateCustomerStatus> {

  public void accept(List<UpdateCustomerStatus> list) {
    list.forEach(this);
  }

  @Override
  public void accept(UpdateCustomerStatus updateCustomerStatus) {
    StringBuilder builder = new StringBuilder();
    if (updateCustomerStatus.getStatus() == null) {
      builder.append("Status is mandatory. ");
    }
    if (updateCustomerStatus.getId() == null) {
      builder.append("Id is mandatory.");
    }
    String exceptionMessage = builder.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }
}
