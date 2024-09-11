package app.bpartners.api.service.invoice;

import static app.bpartners.api.endpoint.rest.model.FileType.INVOICE;
import static app.bpartners.api.endpoint.rest.model.FileType.LOGO;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.api.service.InvoiceService.DRAFT_TEMPLATE;
import static app.bpartners.api.service.InvoiceService.INVOICE_TEMPLATE;

import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.service.FileService;
import java.io.File;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class InvoicePDFProcessor implements Consumer<Invoice> {
  private final InvoicePDFGenerator invoicePDFGenerator;
  private final FileService fileService;

  @Override
  public void accept(Invoice invoice) {
    var fileId = invoice.getFileId();
    if (fileId == null)
      throw new ApiException(
          SERVER_EXCEPTION, "Invoice(id=" + invoice.getId() + ") has null fileId");
    var idUser = invoice.getUser().getId();
    var invoiceFile = process(invoice);

    fileService.upload(INVOICE, fileId, idUser, invoiceFile);
  }

  private File process(Invoice invoice) {
    var invoiceStatus = invoice.getStatus();
    var idUser = invoice.getUser().getId();
    var logoFile = fileService.downloadFile(LOGO, idUser, invoice.getUser().getLogoFileId());
    switch (invoiceStatus) {
      case CONFIRMED, PAID -> {
        return invoicePDFGenerator.apply(
            invoice, invoice.getActualHolder(), logoFile, INVOICE_TEMPLATE);
      }
      default -> {
        return invoicePDFGenerator.apply(
            invoice, invoice.getActualHolder(), logoFile, DRAFT_TEMPLATE);
      }
    }
  }
}
