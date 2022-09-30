package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.Product;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class ProductValidator implements Consumer<Product> {

  @Override
  public void accept(Product product) {
    StringBuilder message = new StringBuilder();
    if (product.getDescription() == null) {
      message.append("Description is mandatory. ");
    }
    if (product.getUnitPrice() == null) {
      message.append("Unit price is mandatory. ");
    }
    if (product.getQuantity() == null) {
      message.append("Quantity is mandatory. ");
    }
    if (product.getVatPercent() == null) {
      message.append("Vat percent is mandatory. ");
    }
    String exceptionMessage = message.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }
}
