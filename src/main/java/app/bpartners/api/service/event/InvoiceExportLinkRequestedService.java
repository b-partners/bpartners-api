package app.bpartners.api.service.event;

import static app.bpartners.api.endpoint.rest.model.ArchiveStatus.ENABLED;
import static app.bpartners.api.endpoint.rest.model.FileType.INVOICE;
import static app.bpartners.api.endpoint.rest.model.FileType.INVOICE_ZIP;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.ACCEPTED;
import static app.bpartners.api.model.BoundedPageSize.MAX_SIZE;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.time.LocalDate.now;
import static java.time.temporal.TemporalAdjusters.lastDayOfMonth;
import static java.util.UUID.randomUUID;

import app.bpartners.api.endpoint.event.model.InvoiceExportLinkRequested;
import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.file.FileZipper;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.service.aws.S3Service;
import app.bpartners.api.service.aws.SesService;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class InvoiceExportLinkRequestedService implements Consumer<InvoiceExportLinkRequested> {
  public static final String PDF_FILE_EXTENSION = ".pdf";
  private final FileZipper fileZipper;
  private final SesService mailer; // TODO: change to Mailer once it works properly !
  private final UserRepository userRepository;
  private final InvoiceRepository invoiceRepository;
  private final S3Service s3Service;

  @SneakyThrows
  @Override
  public void accept(InvoiceExportLinkRequested event) {
    var accountId = event.getAccountId();
    var providedStatuses = event.getProvidedStatuses();
    var providedArchiveStatus = event.getProvidedArchiveStatus();
    var providedFrom = event.getProvidedFrom();
    var providedTo = event.getProvidedTo();
    var page = event.getPage();

    var statuses = getInvoiceStatuses(providedStatuses);
    var archiveStatus = providedArchiveStatus == null ? ENABLED : providedArchiveStatus;
    var from = providedFrom == null ? now().withDayOfMonth(1) : providedFrom;
    var to = providedTo == null ? now().with(lastDayOfMonth()) : providedTo;
    var emptyFilters = new ArrayList<String>();
    var user = userRepository.getByIdAccount(accountId);
    var userId = user.getId();

    var invoices =
        invoiceRepository.findAllByIdUserAndCriteria(
            userId, statuses, archiveStatus, emptyFilters, page, MAX_SIZE);
    var invoicesBetweenDates =
        invoices.stream()
            .filter(
                invoice ->
                    !invoice.getSendingDate().isBefore(from)
                        && !invoice.getSendingDate().isAfter(to))
            .toList();

    var invoicesFiles = downloadInvoicesFiles(userId, invoicesBetweenDates);
    var invoicesZipFile = fileZipper.apply(invoicesFiles);

    var zipFileId = randomUUID().toString();
    s3Service.uploadFile(INVOICE_ZIP, zipFileId, userId, invoicesZipFile);
    long expirationInSeconds = 3600L;
    var preSignedURL = s3Service.presignURL(INVOICE_ZIP, zipFileId, userId, expirationInSeconds);

    var mailSubject =
        "Zip contenant les factures de "
            + user.getDefaultHolder().getName()
            + " entre "
            + providedFrom
            + " et "
            + providedTo
            + " disponible";
    // TODO must be the artisan email and admin as cc
    // TODO var recipient = user.getDefaultHolder().getEmail();
    var adminRecipient = "tech@bpartners.app";
    // TODO: body must be formatted not only preSignedURL
    mailer.sendEmail(adminRecipient, null, mailSubject, preSignedURL);
  }

  @NotNull
  private List<InvoiceStatus> getInvoiceStatuses(List<InvoiceStatus> providedStatuses) {
    var allStatuses = Arrays.stream(InvoiceStatus.values()).toList();
    var entityHandledStatuses =
        allStatuses.stream().filter(status -> !status.equals(ACCEPTED)).toList();
    return providedStatuses == null || providedStatuses.isEmpty()
        ? entityHandledStatuses
        : providedStatuses;
  }

  @NotNull
  private List<File> downloadInvoicesFiles(String userId, List<Invoice> invoicesBetweenDates) {
    return invoicesBetweenDates.stream()
        .map(
            invoice -> {
              File file = s3Service.downloadFile(INVOICE, invoice.getFileId(), userId);
              try {
                Files.move(
                    file.toPath(),
                    Files.createTempFile(invoice.getRef(), PDF_FILE_EXTENSION),
                    REPLACE_EXISTING);
                return file;
              } catch (IOException e) {
                throw new ApiException(SERVER_EXCEPTION, e);
              }
            })
        .toList();
  }
}
