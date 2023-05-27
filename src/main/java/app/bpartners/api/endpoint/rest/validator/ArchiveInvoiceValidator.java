package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.UpdateInvoiceArchivedStatus;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class ArchiveInvoiceValidator implements Consumer<UpdateInvoiceArchivedStatus> {
  @Override
  public void accept(UpdateInvoiceArchivedStatus toArchive) {
    StringBuilder messageBuilder = new StringBuilder();
    if (toArchive.getId() == null) {
      messageBuilder.append("Id is mandatory. ");
    }
    if (toArchive.getArchiveStatus() == null) {
      messageBuilder.append("Status is mandatory. ");
    }
    String errorMessage = messageBuilder.toString();
    if (!errorMessage.isEmpty()) {
      throw new BadRequestException(errorMessage);
    }
  }
}
