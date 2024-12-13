package app.bpartners.api.service.event;

import static app.bpartners.api.endpoint.rest.model.FileType.INVOICE;
import static java.time.Instant.now;

import app.bpartners.api.endpoint.event.model.MonthlySubscriptionInvoiceCreated;
import app.bpartners.api.file.FileWriter;
import app.bpartners.api.model.Attachment;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.service.aws.S3Service;
import app.bpartners.api.service.aws.SesService;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class MonthlySubscriptionInvoiceCreatedService
    implements Consumer<MonthlySubscriptionInvoiceCreated> {
  private final InvoiceRepository invoiceRepository;
  private final SesService mailer;
  private final S3Service s3Service;
  private final FileWriter fileWriter;

  @SneakyThrows
  @Override
  public void accept(MonthlySubscriptionInvoiceCreated event) {
    var invoice = invoiceRepository.findById(event.getInvoiceId());
    var invoiceFile =
        s3Service.downloadFile(INVOICE, invoice.getFileId(), invoice.getUser().getId());
    var attachments =
        List.of(Attachment.builder()
                .name(invoice.getRef())
                .content(fileWriter.writeAsByte(invoiceFile)).build());

    var cc = "contact@bpartners.app"; // TODO: get contact address from env variable
    var recipient = invoice.getCustomer().getEmail();
    var emailSubject = invoice.getTitle();
    var emailBody = ""; // TODO: custom email body

    mailer.sendEmail(recipient, cc, emailSubject, emailBody, attachments);
    log.info("Monthly subscription invoice mail sent to {} at {}", recipient, now());
  }
}
