package app.bpartners.api.endpoint.rest.validator;


import java.util.function.Consumer;
import org.springframework.stereotype.Component;
import app.bpartners.api.endpoint.rest.model.CreateFee;
import app.bpartners.api.model.exception.BadRequestException;

@Component
public class CreateFeeValidator implements Consumer<CreateFee> {
  @Override public void accept(CreateFee createFee) {
    if (createFee.getTotalAmount() == null) {
      throw new BadRequestException("Total amount is mandatory");
    }
  }
}
