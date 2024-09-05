package app.bpartners.api.service.invoice;

import static app.bpartners.api.endpoint.rest.model.FileType.INVOICE;
import static app.bpartners.api.endpoint.rest.model.FileType.LOGO;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;
import static app.bpartners.api.service.InvoiceService.DRAFT_TEMPLATE;
import static app.bpartners.api.service.InvoiceService.INVOICE_TEMPLATE;
import static java.util.UUID.randomUUID;

import app.bpartners.api.model.Invoice;
import app.bpartners.api.service.FileService;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class InvoicePDFProcessor implements Function<Invoice, String> {
  private final InvoicePDFGenerator invoicePDFGenerator;
  private final FileService fileService;

  @Override
  public String apply(Invoice domain) {
    var fileId = domain.getFileId() == null ? String.valueOf(randomUUID()) : domain.getFileId();
    var idUser = domain.getUser().getId();

    var logoAsBytes = fileService.downloadFile(LOGO, idUser, domain.getUser().getLogoFileId());
    var invoiceFile =
        domain.getStatus() == CONFIRMED || domain.getStatus() == PAID
            ? invoicePDFGenerator.apply(
                domain, domain.getActualHolder(), logoAsBytes, INVOICE_TEMPLATE)
            : invoicePDFGenerator.apply(
                domain, domain.getActualHolder(), logoAsBytes, DRAFT_TEMPLATE);

    String actualFileId = fileService.upload(INVOICE, fileId, idUser, invoiceFile).getId();
    domain.setFileId(actualFileId); // TODO: suspicious .. must it really be here ?
    return actualFileId;
  }
}
