package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.CreateProduct;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class CreateProductValidator implements Consumer<CreateProduct> {

  @Override
  public void accept(CreateProduct createProduct) {
    StringBuilder message = new StringBuilder();
    if (createProduct.getDescription() == null) {
      message.append("Description is mandatory. ");
    }
    if (createProduct.getUnitPrice() == null) {
      message.append("Unit price is mandatory. ");
    }
    String exceptionMessage = message.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }
}
