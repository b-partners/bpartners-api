package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.UpdatePaymentRegMethod;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class UpdatePaymentRegValidator implements Consumer<UpdatePaymentRegMethod> {
  @Override
  public void accept(UpdatePaymentRegMethod updatePaymentStatus) {
    if (updatePaymentStatus == null) {
      throw new BadRequestException("UpdatePaymentRegMethod is mandatory");
    } else {
      if (updatePaymentStatus.getMethod() == null) {
        throw new BadRequestException("UpdatePaymentRegMethod.method is mandatory");
      }
    }
  }
}
