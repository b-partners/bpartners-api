package app.bpartners.api.service.subscription;

import static app.bpartners.api.endpoint.rest.model.ArchiveStatus.ENABLED;
import static app.bpartners.api.endpoint.rest.model.FileType.INVOICE;
import static app.bpartners.api.endpoint.rest.model.FileType.INVOICE_ZIP;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;

import app.bpartners.api.file.DirectoryZipper;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.PreSignedLink;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.payment.UserSubscriptionConf;
import app.bpartners.api.repository.jpa.CreditPurchaseRepository;
import app.bpartners.api.repository.model.InvoiceCriteria;
import app.bpartners.api.service.aws.S3Service;
import app.bpartners.api.service.invoice.InvoiceService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionInvoiceExportService {
  public static final String SUBSCRIPTION_FILE_NAME_PREFIX = "Facture abonnement";
  public static final String CREDIT_FILE_NAME_PREFIX = "Facture crédits";
  static final String UNNAMED_CUSTOMER_DIRECTORY = "Client sans nom";
  private static final long EXPIRATION_IN_SECONDS = 3600L;
  private static final String PDF_EXTENSION = ".pdf";
  private static final String ILLEGAL_NAME_CHARACTERS = "[\\\\/:*?\"<>|\\p{Cntrl}]";

  private final InvoiceService invoiceService;
  private final CreditPurchaseRepository creditPurchaseRepository;
  private final UserSubscriptionConf userSubscriptionConf;
  private final DirectoryZipper directoryZipper;
  private final S3Service s3Service;

  public PreSignedLink generateExportLink(LocalDate from, LocalDate to) {
    if (from != null && to != null && to.isBefore(from)) {
      throw new BadRequestException("'from' must be before or equal to 'to'");
    }
    var invoicingUserId = userSubscriptionConf.getUserToCreditId();
    var invoices = findInvoices(invoicingUserId, from, to);
    var exportDirectory = writeCustomerDirectories(invoicingUserId, invoices);
    if (isEmpty(exportDirectory)) {
      throw new NotFoundException("No subscription invoice to export" + periodSuffixOf(from, to));
    }

    var zipFile = directoryZipper.apply(exportDirectory);
    var zipFileId = "factures-abonnements-" + randomUUID() + ".zip";
    s3Service.uploadFile(INVOICE_ZIP, zipFileId, invoicingUserId, zipFile);
    return PreSignedLink.builder()
        .value(s3Service.presignURL(INVOICE_ZIP, zipFileId, invoicingUserId, EXPIRATION_IN_SECONDS))
        .expirationDelay((int) EXPIRATION_IN_SECONDS)
        .updatedAt(now())
        .build();
  }

  private List<Invoice> findInvoices(String invoicingUserId, LocalDate from, LocalDate to) {
    return invoiceService.findAllByCriteria(
        InvoiceCriteria.builder()
            .idUser(invoicingUserId)
            .statusList(List.of(CONFIRMED, PAID))
            .archiveStatus(ENABLED)
            .sendingDateFrom(from)
            .sendingDateTo(to)
            .build());
  }

  @SneakyThrows
  private Path writeCustomerDirectories(String invoicingUserId, List<Invoice> invoices) {
    var creditPurchaseInvoiceIds = Set.copyOf(creditPurchaseRepository.findAllInvoiceIds());
    var rootDirectory = Files.createTempDirectory("subscription-invoices-export-" + randomUUID());
    var takenFileNamesByDirectory = new HashMap<String, Set<String>>();
    for (Invoice invoice : invoices) {
      if (invoice.getFileId() == null) {
        log.warn(
            "Invoice(id={}, ref={}) has no generated PDF, left out of the subscription invoice"
                + " export",
            invoice.getId(),
            invoice.getRef());
        continue;
      }
      var downloaded = download(invoicingUserId, invoice);
      if (downloaded == null) {
        continue;
      }
      var directoryName = customerDirectoryNameOf(invoice);
      var fileName =
          availableName(
              takenFileNamesByDirectory.computeIfAbsent(directoryName, name -> new HashSet<>()),
              fileNameOf(invoice, creditPurchaseInvoiceIds.contains(invoice.getId())));
      var customerDirectory = Files.createDirectories(rootDirectory.resolve(directoryName));
      Files.copy(downloaded, customerDirectory.resolve(fileName), REPLACE_EXISTING);
      Files.deleteIfExists(downloaded);
    }
    return rootDirectory;
  }

  private Path download(String invoicingUserId, Invoice invoice) {
    try {
      return s3Service.downloadFile(INVOICE, invoice.getFileId(), invoicingUserId).toPath();
    } catch (RuntimeException e) {
      log.warn(
          "Could not download the PDF of Invoice(id={}, ref={}, fileId={}), left out of the"
              + " subscription invoice export",
          invoice.getId(),
          invoice.getRef(),
          invoice.getFileId(),
          e);
      return null;
    }
  }

  private String customerDirectoryNameOf(Invoice invoice) {
    var customer = invoice.getCustomer();
    var name = customer == null ? null : customer.getRealName();
    return name == null || name.isBlank()
        ? UNNAMED_CUSTOMER_DIRECTORY
        : sanitize(name.strip(), UNNAMED_CUSTOMER_DIRECTORY);
  }

  private String fileNameOf(Invoice invoice, boolean isCreditPurchaseInvoice) {
    var prefix = isCreditPurchaseInvoice ? CREDIT_FILE_NAME_PREFIX : SUBSCRIPTION_FILE_NAME_PREFIX;
    var reference = invoice.getRef() == null ? invoice.getId() : invoice.getRef();
    return prefix + " " + sanitize(reference, invoice.getId()) + PDF_EXTENSION;
  }

  private String availableName(Set<String> takenNames, String wantedName) {
    var name = wantedName;
    var occurrence = 1;
    while (!takenNames.add(name)) {
      occurrence++;
      name = wantedName.replace(PDF_EXTENSION, " (" + occurrence + ")" + PDF_EXTENSION);
    }
    return name;
  }

  private String sanitize(String name, String fallback) {
    var sanitized = name.replaceAll(ILLEGAL_NAME_CHARACTERS, " ").replaceAll("\\s+", " ").strip();
    return sanitized.isEmpty() || sanitized.equals(".") || sanitized.equals("..")
        ? fallback
        : sanitized;
  }

  @SneakyThrows
  private boolean isEmpty(Path directory) {
    try (var content = Files.list(directory)) {
      return content.findAny().isEmpty();
    }
  }

  private String periodSuffixOf(LocalDate from, LocalDate to) {
    if (from == null && to == null) {
      return "";
    }
    return " between from=" + from + " and to=" + to;
  }
}
