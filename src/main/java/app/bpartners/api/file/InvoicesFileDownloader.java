package app.bpartners.api.file;

import static app.bpartners.api.endpoint.rest.model.FileType.INVOICE;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.UUID.randomUUID;

import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.service.aws.S3Service;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

/** Downloads invoice PDFs into a temp directory, named after the invoice as end users know it. */
@Component
@AllArgsConstructor
public class InvoicesFileDownloader {
  private static final String PDF_EXTENSION = ".pdf";
  private final S3Service s3Service;

  public List<File> apply(String userId, List<Invoice> invoices) {
    Path destinationDirectory = getTempDirectory();
    return invoices.stream()
        .map(invoice -> download(userId, invoice, destinationDirectory))
        .toList();
  }

  private File download(String userId, Invoice invoice, Path destinationDirectory) {
    File file = s3Service.downloadFile(INVOICE, invoice.getFileId(), userId);
    try {
      Path destinationPath = destinationDirectory.resolve(fileNameOf(invoice));
      Files.copy(file.toPath(), destinationPath, REPLACE_EXISTING);
      file.deleteOnExit();
      return destinationPath.toFile();
    } catch (IOException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  private String fileNameOf(Invoice invoice) {
    var customer = invoice.getCustomer();
    var customerName = customer.getName() == null ? customer.getFullName() : customer.getName();
    return invoice.getRef()
        + " "
        + customerName
        + " "
        + getInvoiceYearMonth(invoice.getSendingDate())
        + PDF_EXTENSION;
  }

  private String getInvoiceYearMonth(LocalDate sendingDate) {
    return getMonthFrenchTranslation(sendingDate.getMonth()) + " " + sendingDate.getYear();
  }

  private String getMonthFrenchTranslation(Month month) {
    return switch (month) {
      case JANUARY -> "Janvier";
      case FEBRUARY -> "Février";
      case MARCH -> "Mars";
      case APRIL -> "Avril";
      case MAY -> "Mai";
      case JUNE -> "Juin";
      case JULY -> "Juillet";
      case AUGUST -> "Août";
      case SEPTEMBER -> "Septembre";
      case OCTOBER -> "Octobre";
      case NOVEMBER -> "Novembre";
      case DECEMBER -> "Décembre";
      case null -> throw new IllegalArgumentException("Invalid month");
    };
  }

  @SneakyThrows
  private static Path getTempDirectory() {
    return Files.createTempDirectory("tmp" + randomUUID());
  }
}
