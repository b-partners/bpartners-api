package app.bpartners.api.service;

import app.bpartners.api.endpoint.event.model.gen.InvoiceRelaunchSaved;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.Attachment;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.utils.InvoicePdfUtils;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javax.mail.MessagingException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import static app.bpartners.api.endpoint.rest.model.FileType.ATTACHMENT;
import static app.bpartners.api.endpoint.rest.model.FileType.LOGO;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;
import static app.bpartners.api.service.InvoiceCrupdatedService.DRAFT_TEMPLATE;
import static app.bpartners.api.service.InvoiceCrupdatedService.INVOICE_TEMPLATE;

@Service
@AllArgsConstructor
@Slf4j
public class InvoiceRelaunchSavedService implements Consumer<InvoiceRelaunchSaved> {
  private final SesService service;
  private final FileService fileService;
  private final InvoicePdfUtils pdfUtils = new InvoicePdfUtils();

  @Transactional(isolation = Isolation.SERIALIZABLE)
  @Override
  public void accept(InvoiceRelaunchSaved invoiceRelaunchSaved) {
    String recipient = invoiceRelaunchSaved.getRecipient();
    String subject = invoiceRelaunchSaved.getSubject();
    String htmlBody = invoiceRelaunchSaved.getHtmlBody();
    String attachmentName = invoiceRelaunchSaved.getAttachmentName();
    Invoice invoice = invoiceRelaunchSaved.getInvoice();
    AccountHolder accountHolder = invoiceRelaunchSaved.getAccountHolder();
    String logoFileId = invoiceRelaunchSaved.getLogoFileId();
    List<byte[]> logoFiles = fileService.downloadOptionalFile(
        LOGO, invoice.getActualAccount().getId(), logoFileId);
    byte[] logoAsBytes = logoFiles.isEmpty()
        ? new byte[0]
        : logoFiles.get(0);

    byte[] attachmentAsBytes =
        invoice.getStatus().equals(CONFIRMED) || invoice.getStatus().equals(PAID)
            ? pdfUtils.generatePdf(invoice, accountHolder, logoAsBytes,
            INVOICE_TEMPLATE)
            : pdfUtils.generatePdf(invoice, accountHolder, logoAsBytes,
            DRAFT_TEMPLATE);
    Attachment attachment = Attachment.builder()
        .name(attachmentName)
        .content(attachmentAsBytes)
        .build();
    List<Attachment> attachments = new ArrayList<>(invoiceRelaunchSaved.getAttachments());
    attachments.forEach(contentlessAttachment -> {
      byte[] content = fileService.downloadFile(
          ATTACHMENT,
          invoice.getActualAccount().getId(),
          contentlessAttachment.getFileId()
      );
      contentlessAttachment.setContent(content);
    });
    attachments.add(attachment);
    invoiceRelaunchSaved.setAttachments(attachments);
    try {
      service.sendEmail(recipient, subject, htmlBody, attachments);
      log.info("Email sent from "
          + invoice.getActualAccount().describeMinInfos() + " to " + recipient);
    } catch (MessagingException | IOException e) {
      log.error("Email not sent : " + e.getMessage());
    }
  }
}
