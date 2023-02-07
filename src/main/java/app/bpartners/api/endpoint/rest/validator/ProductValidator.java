package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.Product;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
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
      log.warn("DEPRECATED : Vat percent is mandatory. 0% by default is now attribute.");
      //TODO: uncomment when any log is shown anymore
      product.setVatPercent(0);
      //message.append("Vat percent is mandatory. ");
    }
    String exceptionMessage = message.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }
}
