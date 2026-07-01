package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.UpdateInvoiceStatus;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.List;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class UpdateInvoiceStatusRestValidator implements Consumer<UpdateInvoiceStatus> {

  public void accept(List<UpdateInvoiceStatus> updateStatusList) {
    updateStatusList.forEach(this);
  }

  @Override
  public void accept(UpdateInvoiceStatus updateStatus) {
    StringBuilder exceptionMessageBuilder = new StringBuilder();
    if (updateStatus.getInvoiceStatus() == null) {
      exceptionMessageBuilder.append("InvoiceStatus is mandatory. ");
    }
    if (updateStatus.getInvoiceIdentifier() != null && updateStatus.getInvoiceReference() != null) {
      exceptionMessageBuilder.append(
          "Both invoiceIdentifier and invoiceReference can not be provided at same time.");
    }
    var exceptionBuilt = exceptionMessageBuilder.toString();

    if (!exceptionBuilt.isEmpty()) {
      throw new BadRequestException(exceptionBuilt);
    }
  }
}
