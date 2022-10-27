package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.CreateInvoiceRelaunch;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class CreateInvoiceRelaunchValidator implements Consumer<CreateInvoiceRelaunch> {
  @Override
  public void accept(CreateInvoiceRelaunch invoiceRelaunch) {
    StringBuilder exceptionMessageBuilder = new StringBuilder();
    if (invoiceRelaunch.getDraftRelaunch() == null) {
      exceptionMessageBuilder.append("Draft relaunch is mandatory. ");
    } else {
      if (invoiceRelaunch.getDraftRelaunch() <= 0) {
        exceptionMessageBuilder.append("Draft relaunch must be higher than 0. ");
      }
    }
    if (invoiceRelaunch.getUnpaidRelaunch() == null) {
      exceptionMessageBuilder.append("Unpaid relaunch is mandatory. ");
    } else {
      if (invoiceRelaunch.getUnpaidRelaunch() <= 0) {
        exceptionMessageBuilder.append("Unpaid relaunch must be higher than 0. ");
      }
    }
    String exceptionMessage = exceptionMessageBuilder.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }
}