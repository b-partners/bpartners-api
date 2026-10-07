package app.bpartners.api.service.subscription;

import static app.bpartners.api.endpoint.rest.model.FileType.INVOICE;
import static app.bpartners.api.endpoint.rest.model.FileType.INVOICE_ZIP;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.file.DirectoryZipper;
import app.bpartners.api.model.Customer;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.payment.UserSubscriptionConf;
import app.bpartners.api.repository.jpa.CreditPurchaseRepository;
import app.bpartners.api.repository.model.InvoiceCriteria;
import app.bpartners.api.service.aws.S3Service;
import app.bpartners.api.service.invoice.InvoiceService;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SubscriptionInvoiceExportServiceTest {
  private static final String USER_TO_CREDIT_ID = "user_to_credit_id";

  InvoiceService invoiceServiceMock = mock();
  CreditPurchaseRepository creditPurchaseRepositoryMock = mock();
  UserSubscriptionConf userSubscriptionConfMock = mock();
  S3Service s3ServiceMock = mock();

  SubscriptionInvoiceExportService subject =
      new SubscriptionInvoiceExportService(
          invoiceServiceMock,
          creditPurchaseRepositoryMock,
          userSubscriptionConfMock,
          new DirectoryZipper(),
          s3ServiceMock);

  @BeforeEach
  void setUp() {
    when(userSubscriptionConfMock.getUserToCreditId()).thenReturn(USER_TO_CREDIT_ID);
    when(creditPurchaseRepositoryMock.findAllInvoiceIds()).thenReturn(List.of());
    when(invoiceServiceMock.findAllByCriteria(any())).thenReturn(List.of());
    when(s3ServiceMock.downloadFile(eq(INVOICE), anyString(), eq(USER_TO_CREDIT_ID)))
        .thenAnswer(invocation -> pdfNamed(invocation.getArgument(1)));
    when(s3ServiceMock.presignURL(eq(INVOICE_ZIP), anyString(), anyString(), anyLong()))
        .thenReturn("https://s3/factures-abonnements.zip");
  }

  @Test
  void exports_one_folder_per_billed_customer() {
    when(creditPurchaseRepositoryMock.findAllInvoiceIds()).thenReturn(List.of("invoice_2"));
    when(invoiceServiceMock.findAllByCriteria(any()))
        .thenReturn(
            List.of(
                invoice("invoice_1", "REF-11111", "Toiture SARL"),
                invoice("invoice_2", "REF-22222", "Toiture SARL"),
                invoice("invoice_3", "REF-33333", "Couvreur SAS")));

    var preSignedLink = subject.generateExportLink(null, null);

    assertEquals("https://s3/factures-abonnements.zip", preSignedLink.getValue());
    assertEquals(3600, preSignedLink.getExpirationDelay());
    assertEquals(
        List.of(
            "Couvreur SAS/Facture abonnement REF-33333.pdf",
            "Toiture SARL/Facture abonnement REF-11111.pdf",
            "Toiture SARL/Facture crédits REF-22222.pdf"),
        uploadedZipEntries());
  }

  @Test
  void does_not_export_a_folder_for_a_customer_without_any_downloadable_invoice() {
    when(invoiceServiceMock.findAllByCriteria(any()))
        .thenReturn(
            List.of(
                invoice("invoice_1", "REF-11111", "Toiture SARL"),
                invoice("invoice_2", "REF-22222", "Sans facture SARL").toBuilder()
                    .fileId(null)
                    .build(),
                invoice("invoice_3", "REF-33333", "Perdue SAS")));
    when(s3ServiceMock.downloadFile(INVOICE, "file_invoice_3", USER_TO_CREDIT_ID))
        .thenThrow(new RuntimeException("no such key"));

    subject.generateExportLink(null, null);

    assertEquals(List.of("Toiture SARL/Facture abonnement REF-11111.pdf"), uploadedZipEntries());
  }

  @Test
  void exports_the_invoices_sent_over_the_requested_period_only() {
    when(invoiceServiceMock.findAllByCriteria(any()))
        .thenReturn(List.of(invoice("invoice_1", "REF-11111", "Toiture SARL")));
    var from = LocalDate.of(2026, 1, 1);
    var to = LocalDate.of(2026, 1, 31);

    subject.generateExportLink(from, to);

    var criteriaCaptor = ArgumentCaptor.forClass(InvoiceCriteria.class);
    verify(invoiceServiceMock).findAllByCriteria(criteriaCaptor.capture());
    var criteria = criteriaCaptor.getValue();
    assertEquals(USER_TO_CREDIT_ID, criteria.idUser());
    assertEquals(from, criteria.sendingDateFrom());
    assertEquals(to, criteria.sendingDateTo());
  }

  @Test
  void refuses_a_period_ending_before_it_starts() {
    assertThrows(
        BadRequestException.class,
        () -> subject.generateExportLink(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1)));
  }

  @Test
  void refuses_to_export_an_empty_archive() {
    assertThrows(NotFoundException.class, () -> subject.generateExportLink(null, null));
  }

  @Test
  void keeps_both_invoices_sharing_a_reference_inside_a_customer_folder() {
    when(invoiceServiceMock.findAllByCriteria(any()))
        .thenReturn(
            List.of(
                invoice("invoice_1", "REF-11111", "Toiture SARL"),
                invoice("invoice_2", "REF-11111", "Toiture SARL")));

    subject.generateExportLink(null, null);

    assertEquals(
        List.of(
            "Toiture SARL/Facture abonnement REF-11111 (2).pdf",
            "Toiture SARL/Facture abonnement REF-11111.pdf"),
        uploadedZipEntries());
  }

  @Test
  void replaces_the_path_separators_of_a_customer_name() {
    when(invoiceServiceMock.findAllByCriteria(any()))
        .thenReturn(List.of(invoice("invoice_1", "REF-11111", "Toiture/Couverture SARL")));

    subject.generateExportLink(null, null);

    assertEquals(
        List.of("Toiture Couverture SARL/Facture abonnement REF-11111.pdf"), uploadedZipEntries());
  }

  private List<String> uploadedZipEntries() {
    var zipCaptor = ArgumentCaptor.forClass(File.class);
    verify(s3ServiceMock)
        .uploadFile(eq(INVOICE_ZIP), anyString(), eq(USER_TO_CREDIT_ID), zipCaptor.capture());
    return zipEntriesOf(zipCaptor.getValue());
  }

  private static List<String> zipEntriesOf(File zipFile) {
    try (var zip = new ZipFile(zipFile)) {
      var entries = new ArrayList<String>();
      zip.stream().forEach(entry -> entries.add(entry.getName()));
      entries.sort(String::compareTo);
      return entries;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private static File pdfNamed(String fileId) throws Exception {
    var file = Files.createTempFile(fileId, ".pdf");
    Files.write(file, ("pdf of " + fileId).getBytes(StandardCharsets.UTF_8));
    return file.toFile();
  }

  private static Invoice invoice(String id, String ref, String customerName) {
    return Invoice.builder()
        .id(id)
        .ref(ref)
        .fileId("file_" + id)
        .customer(Customer.builder().id("customer_" + customerName).name(customerName).build())
        .build();
  }
}
