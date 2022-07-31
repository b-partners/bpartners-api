package app.bpartners.api.endpoint.rest.validator;

import java.util.function.Consumer;
import org.springframework.stereotype.Component;
import app.bpartners.api.endpoint.rest.model.CreatePayment;
import app.bpartners.api.model.exception.BadRequestException;

@Component
public class CreatePaymentValidator implements Consumer<CreatePayment> {
  @Override public void accept(CreatePayment createPayment) {
    if (createPayment.getAmount() == null) {
      throw new BadRequestException("Amount is mandatory");
    }
  }
}