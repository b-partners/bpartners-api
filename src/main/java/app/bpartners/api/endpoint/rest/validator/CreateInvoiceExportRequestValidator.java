package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.CreateInvoiceExportRequest;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class CreateInvoiceExportRequestValidator implements Consumer<CreateInvoiceExportRequest> {

  @Override
  public void accept(CreateInvoiceExportRequest createInvoiceExportRequest) {
    StringBuilder exceptionMessageBuilder = new StringBuilder();
    if (createInvoiceExportRequest.getFrom() == null) {
      exceptionMessageBuilder.append("CreateInvoiceExportRequest.from is mandatory. ");
    }
    if (createInvoiceExportRequest.getTo() == null) {
      exceptionMessageBuilder.append("CreateInvoiceExportRequest.to is mandatory. ");
    }
    if (createInvoiceExportRequest.getFrom() != null
        && createInvoiceExportRequest.getTo() != null
        && createInvoiceExportRequest.getFrom().isAfter(createInvoiceExportRequest.getTo())) {
      exceptionMessageBuilder.append(
          "CreateInvoiceExportRequest.from can not be after CreateInvoiceExportRequest.to.");
    }
    if (createInvoiceExportRequest.getArchiveStatus() == null) {
      exceptionMessageBuilder.append("CreateInvoiceExportRequest.archiveStatus is mandatory. ");
    }
    if (createInvoiceExportRequest.getStatusList() == null
        || createInvoiceExportRequest.getStatusList().isEmpty()) {
      exceptionMessageBuilder.append(
          "CreateInvoiceExportRequest.statusList is mandatory and can not be empty. ");
    }
    var exceptionMessage = exceptionMessageBuilder.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }
}
