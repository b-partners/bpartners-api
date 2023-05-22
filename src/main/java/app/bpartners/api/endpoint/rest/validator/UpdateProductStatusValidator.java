package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.UpdateProductStatus;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class UpdateProductStatusValidator implements Consumer<UpdateProductStatus> {

  public void accept(List<UpdateProductStatus> list) {
    list.forEach(this);
  }

  @Override
  public void accept(UpdateProductStatus productStatus) {
    StringBuilder builder = new StringBuilder();
    if (productStatus.getStatus() == null) {
      builder.append("Status is mandatory. ");
    }
    if (productStatus.getId() == null) {
      builder.append("Id is mandatory.");
    }
    String exceptionMessage = builder.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }
}
