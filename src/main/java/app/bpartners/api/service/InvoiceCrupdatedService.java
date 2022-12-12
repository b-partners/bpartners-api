package app.bpartners.api.service;

import app.bpartners.api.endpoint.event.model.gen.InvoiceCrupdated;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.FileInfo;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.repository.jpa.InvoiceJpaRepository;
import app.bpartners.api.repository.jpa.model.HInvoice;
import app.bpartners.api.service.utils.InvoicePdfUtils;
import java.util.function.Consumer;
import javax.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import static app.bpartners.api.endpoint.rest.model.FileType.INVOICE;
import static app.bpartners.api.endpoint.rest.model.FileType.LOGO;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;
import static app.bpartners.api.service.utils.FileInfoUtils.PDF_EXTENSION;
import static java.util.UUID.randomUUID;

@Service
@AllArgsConstructor
public class InvoiceCrupdatedService implements Consumer<InvoiceCrupdated> {
  public static final String INVOICE_TEMPLATE = "invoice";
  public static final String DRAFT_TEMPLATE = "draft";
  private final FileService fileService;
  private final InvoiceJpaRepository invoiceJpaRepository;

  @Transactional
  @Override
  public void accept(InvoiceCrupdated invoiceCrupdated) {
    InvoicePdfUtils pdfUtils = new InvoicePdfUtils();
    AccountHolder accountHolder = invoiceCrupdated.getAccountHolder();
    String logoFileId = invoiceCrupdated.getLogoFileId();
    Invoice invoice = invoiceCrupdated.getInvoice();
    String accountId = invoice.getAccount().getId();
    String fileId =
        invoice.getFileId() == null ? randomUUID() + PDF_EXTENSION : invoice.getFileId();
    byte[] logoAsBytes = fileService.downloadOptionalFile(LOGO, accountId, logoFileId);
    byte[] fileAsBytes =
        invoice.getStatus().equals(CONFIRMED) || invoice.getStatus().equals(PAID)
            ? pdfUtils.generatePdf(invoice, accountHolder, logoAsBytes, INVOICE_TEMPLATE)
            : pdfUtils.generatePdf(invoice, accountHolder, logoAsBytes, DRAFT_TEMPLATE);

    FileInfo fileInfo = fileService.upload(fileId, INVOICE, accountId, fileAsBytes, null);
    invoiceJpaRepository.save(HInvoice.builder()
        .id(invoice.getId())
        .fileId(fileInfo.getId())
        .comment(invoice.getComment())
        .ref(invoice.getRealReference())
        .title(invoice.getTitle())
        .idAccount(invoice.getAccount().getId())
        .sendingDate(invoice.getSendingDate())
        .toPayAt(invoice.getToPayAt())
        .status(invoice.getStatus())
        .build());
  }
}
