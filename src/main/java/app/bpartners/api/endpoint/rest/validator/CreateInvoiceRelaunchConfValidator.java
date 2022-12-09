package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.CreateAccountInvoiceRelaunchConf;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class CreateInvoiceRelaunchConfValidator
    implements Consumer<CreateAccountInvoiceRelaunchConf> {
  @Override
  public void accept(CreateAccountInvoiceRelaunchConf createAccountInvoiceRelaunchConf) {
    StringBuilder exceptionMessageBuilder = new StringBuilder();
    if (createAccountInvoiceRelaunchConf.getDraftRelaunch() == null) {
      exceptionMessageBuilder.append("Draft relaunch is mandatory. ");
    } else if (createAccountInvoiceRelaunchConf.getDraftRelaunch() <= 0) {
      exceptionMessageBuilder.append("Draft relaunch must be higher than 0. ");
    }
    if (createAccountInvoiceRelaunchConf.getUnpaidRelaunch() == null) {
      exceptionMessageBuilder.append("Unpaid relaunch is mandatory. ");
    } else if (createAccountInvoiceRelaunchConf.getUnpaidRelaunch() <= 0) {
      exceptionMessageBuilder.append("Unpaid relaunch must be higher than 0. ");
    }
    String exceptionMessage = exceptionMessageBuilder.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }
}