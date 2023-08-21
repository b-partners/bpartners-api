package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.UpdatePaymentStatus;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class UpdatePaymentValidator implements Consumer<UpdatePaymentStatus> {
  @Override
  public void accept(UpdatePaymentStatus updatePaymentStatus) {
    if (updatePaymentStatus == null) {
      throw new BadRequestException("UpdatePaymentStatus is mandatory");
    } else {
      if (updatePaymentStatus.getStatus() == null) {
        throw new BadRequestException("UpdatePaymentStatus.status is mandatory");
      }
    }
  }
}
