package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.CreateProduct;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
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
    if (createProduct.getVatPercent() == null) {
      log.warn("DEPRECATED : Vat percent is mandatory. 0% by default is now attribute.");
      //TODO: uncomment when any log is shown anymore
      createProduct.setVatPercent(0);
      //message.append("Vat percent is mandatory. ");
    }
    String exceptionMessage = message.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }
}
