package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.TransactionExportInput;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class TransactionExportValidator implements Consumer<TransactionExportInput> {
  @Override
  public void accept(TransactionExportInput input) {
    StringBuilder sb = new StringBuilder();
    if (input == null) {
      sb.append("RequestBody `TransactionExportInput` is mandatory");
    } else {
      if (input.getFrom() == null) {
        sb.append("Attribute `from` is mandatory. ");
      }
      if (input.getTo() == null) {
        sb.append("Attribute `to` is mandatory. ");
      }
    }
    String exceptionMsg = sb.toString();
    if (!exceptionMsg.isEmpty()) {
      throw new BadRequestException(exceptionMsg);
    }
  }
}
