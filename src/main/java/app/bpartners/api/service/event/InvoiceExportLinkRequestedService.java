package app.bpartners.api.service.event;

import static app.bpartners.api.endpoint.rest.model.FileType.INVOICE_ZIP;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.api.service.invoice.InvoiceService.MAX_PAGE;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.InvoiceExportLinkRequested;
import app.bpartners.api.file.FileZipper;
import app.bpartners.api.file.InvoicesFileDownloader;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.service.aws.S3Service;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.utils.TemplateResolverEngine;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;
import javax.mail.MessagingException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;

@Slf4j
@Service
@AllArgsConstructor
public class InvoiceExportLinkRequestedService implements Consumer<InvoiceExportLinkRequested> {
  public static final String INVOICE_EXPORT_LINK_REQUESTED_BODY = "invoice_export_link_requested";
  private static final long EXPIRATION_IN_SECONDS = 3600L;
  private final FileZipper fileZipper;
  private final InvoicesFileDownloader invoicesFileDownloader;
  private final SesService mailer;
  private final UserRepository userRepository;
  private final InvoiceRepository invoiceRepository;
  private final S3Service s3Service;
  private final TemplateResolverEngine templateResolverEngine;
  private final EventProducer<InvoiceExportLinkRequested> eventProducer;

  @Override
  public void accept(InvoiceExportLinkRequested event) {
    var userId = event.getUserId();
    var from = event.getProvidedFrom();
    var to = event.getProvidedTo();
    var page = event.getPage();
    var totalPage = event.getTotalPage();
    var providedStatus = event.getProvidedStatuses();
    var archiveStatus = event.getProvidedArchiveStatus();
    List<Invoice> invoices =
        invoiceRepository.findAllByIdUserAndSendingDateBetweenAndPaginate(
            userId, from, to, page, MAX_PAGE);
    var zipFileId = accept(userId, invoices);
    var preSignedURL = s3Service.presignURL(INVOICE_ZIP, zipFileId, userId, EXPIRATION_IN_SECONDS);
    var user = userRepository.getById(userId);
    String htmlBody =
        templateResolverEngine.parseTemplateResolver(
            INVOICE_EXPORT_LINK_REQUESTED_BODY,
            configureInvoiceLinkContext(user, from, to, preSignedURL));
    var mailSubject =
        String.format(
            "Ensemble des factures de l'utilisateur: %s - Partie %s / %s",
            user.getDefaultHolder().getName(), page + 1, totalPage);
    var adminRecipient = "tech@birdia.fr";
    try {
      mailer.sendEmail(adminRecipient, adminRecipient, mailSubject, htmlBody);
      if (page < totalPage && totalPage != 1) {
        eventProducer.accept(
            List.of(
                InvoiceExportLinkRequested.builder()
                    .userId(userId)
                    .providedStatuses(providedStatus)
                    .providedArchiveStatus(archiveStatus)
                    .providedFrom(from)
                    .providedTo(to)
                    .page(page + 1)
                    .totalPage(totalPage)
                    .build()));
      }
    } catch (IOException | MessagingException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  private @NotNull String accept(String userId, List<Invoice> invoices) {
    var invoicesFiles = invoicesFileDownloader.apply(userId, invoices);
    var invoicesZipFile = fileZipper.apply(invoicesFiles);
    var zipFileId = invoicesZipFile.getName();
    s3Service.uploadFile(INVOICE_ZIP, zipFileId, userId, invoicesZipFile);
    return zipFileId;
  }

  private Context configureInvoiceLinkContext(
      User user, LocalDate from, LocalDate to, String preSignedURL) {
    Context context = new Context();
    context.setVariable("userName", user.getDefaultHolder().getName());
    context.setVariable("from", from);
    context.setVariable("to", to);
    context.setVariable("exportedLink", preSignedURL);
    return context;
  }
}
