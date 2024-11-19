package app.bpartners.api.service.event;

import static app.bpartners.api.endpoint.rest.model.FileType.INVOICE;
import static app.bpartners.api.endpoint.rest.model.FileType.INVOICE_ZIP;
import static app.bpartners.api.model.BoundedPageSize.MAX_SIZE;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.time.LocalDate.now;
import static java.util.UUID.randomUUID;

import app.bpartners.api.endpoint.event.model.InvoiceExportLinkRequested;
import app.bpartners.api.file.FileZipper;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.jpa.InvoiceJpaRepository;
import app.bpartners.api.service.aws.S3Service;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.utils.TemplateResolverEngine;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Consumer;
import javax.mail.MessagingException;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
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
  private final SesService mailer;
  private final UserRepository userRepository;
  private final InvoiceRepository invoiceRepository;
  private final S3Service s3Service;
  private final TemplateResolverEngine templateResolverEngine;
  private final InvoiceJpaRepository invoiceJpaRepository;

  @Override
  public void accept(InvoiceExportLinkRequested event) {
    var accountId = event.getAccountId();
    var providedFrom = event.getProvidedFrom();
    var providedTo = event.getProvidedTo();

    var from = providedFrom == null ? now() : providedFrom;
    var to = providedTo == null ? now() : providedTo;
    var user = userRepository.getByIdAccount(accountId);
    var userId = user.getId();
    var totalInvoices =
        invoiceJpaRepository.countAllByIdUserAndSendingDateBetween(userId, from, to);
    var nbPage = Math.max(1, (int) Math.ceil((double) totalInvoices / MAX_SIZE));
    String htmlBody;
    var zipFileId = randomUUID().toString();
    String preSignedURL;

    if (totalInvoices > 0) {
      Path invoicesFiles = getTempDirectory();
      for (int page = 0; page < nbPage; page++) {
        List<Invoice> invoicePaginate =
            invoiceRepository.findAllByIdUserAndSendingDateBetweenAndPaginate(
                userId, from, to, page, MAX_SIZE);

        var invoicePaginateFile = downloadInvoicesFiles(userId, invoicePaginate);

        for (File invoice : invoicePaginateFile) {
          try {
            Files.copy(invoice.toPath(), invoicesFiles, REPLACE_EXISTING);
            Files.delete(invoice.toPath());
          } catch (IOException e) {
            log.info("Exception={}", e.getMessage());
            throw new RuntimeException(e);
          }
        }
      }
      var invoicesZipFile = fileZipper.apply(List.of(invoicesFiles.toFile()));
      s3Service.uploadFile(INVOICE_ZIP, zipFileId, userId, invoicesZipFile);
      preSignedURL = s3Service.presignURL(INVOICE_ZIP, zipFileId, userId, EXPIRATION_IN_SECONDS);
    } else {
      preSignedURL = "Aucune facture ne correspond aux critères recherchés.";
      log.info("preSignedUrl={}", preSignedURL);
    }

    htmlBody =
        templateResolverEngine.parseTemplateResolver(
            INVOICE_EXPORT_LINK_REQUESTED_BODY,
            configureInvoiceLinkContext(user, providedFrom, providedTo, preSignedURL));
    var mailSubject =
        "Ensemble des factures de l'utilisateur: " + user.getDefaultHolder().getName() + ".";
    var recipient = user.getDefaultHolder().getEmail();
    var adminRecipient = "tech@bpartners.app";
    try {
      mailer.sendEmail(recipient, adminRecipient, mailSubject, htmlBody);
    } catch (IOException | MessagingException e) {
      log.info("Exception={}", e.getMessage());
      throw new RuntimeException(e);
    }
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

  @NotNull
  private List<File> downloadInvoicesFiles(String userId, List<Invoice> invoicesBetweenDates) {
    Path destinationDirectory = getTempDirectory();
    return invoicesBetweenDates.stream()
        .map(
            invoice -> {
              File file = s3Service.downloadFile(INVOICE, invoice.getFileId(), userId);
              try {
                Path destinationPath = destinationDirectory.resolve(invoice.getRef());
                Files.copy(file.toPath(), destinationPath, REPLACE_EXISTING);
                Files.delete(file.toPath());
                return destinationPath.toFile();
              } catch (IOException e) {
                throw new ApiException(SERVER_EXCEPTION, e);
              }
            })
        .toList();
  }

  @SneakyThrows
  private static Path getTempDirectory() {
    return Files.createTempDirectory("tmp" + randomUUID());
  }
}
