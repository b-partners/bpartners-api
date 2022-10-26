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
    if (invoiceRelaunch.getDraftRelaunch() == null || invoiceRelaunch.getDraftRelaunch() <= 0) {
      exceptionMessageBuilder.append("draftRelaunch cannot be null or negative. ");
    }
    if (invoiceRelaunch.getUnpaidRelaunch() == null || invoiceRelaunch.getUnpaidRelaunch() <= 0) {
      exceptionMessageBuilder.append("unpaidRelaunch cannot be null or negative. ");
    }
    String exceptionMessage = exceptionMessageBuilder.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }
}