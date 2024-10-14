package app.bpartners.api.unit.service;

import static app.bpartners.api.endpoint.rest.model.ArchiveStatus.ENABLED;
import static app.bpartners.api.endpoint.rest.model.FileType.INVOICE;
import static app.bpartners.api.endpoint.rest.model.FileType.INVOICE_ZIP;
import static app.bpartners.api.file.FileHashAlgorithm.SHA256;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.file.FileHash;
import app.bpartners.api.file.FileZipper;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.PreSignedLink;
import app.bpartners.api.model.User;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.repository.PaymentRequestRepository;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.service.InvoiceService;
import app.bpartners.api.service.PaymentInitiationService;
import app.bpartners.api.service.aws.S3Service;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.invoice.CustomerInvoiceValidator;
import app.bpartners.api.service.invoice.InvoicePDFProcessor;
import app.bpartners.api.service.invoice.InvoiceValidator;
import app.bpartners.api.service.payment.CreatePaymentRegulationComputing;
import app.bpartners.api.service.payment.PaymentService;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.zip.ZipFile;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class InvoiceServiceTest {
  private static final String USER_ID = "userId";
  private static final String ACCOUNT_ID = "accountId";
  private static final String PRE_SIGNED_URL = "preSignedURL";
  private static final long DEFAULT_EXPIRATION_DELAY = 3600L;

  InvoiceRepository repositoryMock = mock();
  PaymentInitiationService initiationServiceMock = mock();
  PaymentRequestRepository paymentRequestRepositoryMock = mock();
  InvoicePDFProcessor invoicePDFProcessorMock = mock();
  CreatePaymentRegulationComputing regulationComputingMock = mock();
  PaymentService paymentServiceMock = mock();
  InvoiceValidator invoiceValidatorMock = mock();
  CustomerInvoiceValidator customerInvoiceValidatorMock = mock();
  S3Service s3ServiceMock = mock();
  FileZipper fileZipper = new FileZipper();
  SesService mailerMock = mock();
  UserRepository userRepositoryMock = mock();
  InvoiceService subject =
      new InvoiceService(
          repositoryMock,
          initiationServiceMock,
          paymentRequestRepositoryMock,
          invoicePDFProcessorMock,
          regulationComputingMock,
          paymentServiceMock,
          invoiceValidatorMock,
          customerInvoiceValidatorMock,
          s3ServiceMock,
          fileZipper,
          mailerMock,
          userRepositoryMock);

  @BeforeEach
  @SneakyThrows
  void setUp() {
    when(userRepositoryMock.getByIdAccount(ACCOUNT_ID)).thenReturn(actualUser());
    when(s3ServiceMock.uploadFile(eq(INVOICE_ZIP), any(String.class), eq(USER_ID), any(File.class)))
        .thenReturn(new FileHash(SHA256, "fileHash"));
    when(s3ServiceMock.presignURL(
            eq(INVOICE_ZIP), any(String.class), eq(USER_ID), eq(DEFAULT_EXPIRATION_DELAY)))
        .thenReturn(PRE_SIGNED_URL);
    when(s3ServiceMock.downloadFile(eq(INVOICE), any(String.class), eq(USER_ID)))
        .thenReturn(File.createTempFile(randomUUID().toString(), randomUUID().toString()));
    doNothing().when(mailerMock).sendEmail(any(), any(), any(), any());
  }

  private User actualUser() {
    return User.builder().id(USER_ID).build();
  }

  @Test
  void generate_export_link_with_empty_invoices_ok() {
    when(repositoryMock.findAllByIdUserAndCriteria(
            eq(USER_ID), anyList(), eq(ENABLED), anyList(), anyInt(), anyInt()))
        .thenReturn(List.of());
    LocalDate today = LocalDate.now();
    List<InvoiceStatus> providedStatuses = List.of();

    PreSignedLink actual =
        subject.generateInvoicesExportLink(
            ACCOUNT_ID, providedStatuses, ENABLED, today, today.plusDays(1L));

    var fileCaptor = ArgumentCaptor.forClass(File.class);
    verify(s3ServiceMock).uploadFile(any(), any(), any(), fileCaptor.capture());
    Long invoicesCount;
    try (var invoiceZipFile = new ZipFile(fileCaptor.getValue())) {
      invoicesCount = invoiceZipFile.stream().count();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    assertEquals(0L, invoicesCount);
    assertNotNull(actual.getUpdatedAt());
    assertEquals(
        PreSignedLink.builder()
            .value(PRE_SIGNED_URL)
            .expirationDelay((int) DEFAULT_EXPIRATION_DELAY)
            .updatedAt(actual.getUpdatedAt())
            .build(),
        actual);
  }

  @Test
  void generate_export_link_with_invoices_ok() {
    LocalDate today = LocalDate.now();
    when(repositoryMock.findAllByIdUserAndCriteria(
            eq(USER_ID), anyList(), eq(ENABLED), anyList(), anyInt(), anyInt()))
        .thenReturn(
            List.of(
                Invoice.builder()
                    .id(randomUUID().toString())
                    .fileId(randomUUID().toString())
                    .sendingDate(today)
                    .user(actualUser())
                    .build(),
                Invoice.builder()
                    .id(randomUUID().toString())
                    .fileId(randomUUID().toString())
                    .sendingDate(today)
                    .user(actualUser())
                    .build()));
    List<InvoiceStatus> providedStatuses = List.of();

    PreSignedLink actual =
        subject.generateInvoicesExportLink(
            ACCOUNT_ID, providedStatuses, ENABLED, today, today.plusDays(1L));

    var fileCaptor = ArgumentCaptor.forClass(File.class);
    verify(s3ServiceMock).uploadFile(any(), any(), any(), fileCaptor.capture());
    Long invoicesCount;
    try (var invoiceZipFile = new ZipFile(fileCaptor.getValue())) {
      invoicesCount = invoiceZipFile.stream().count();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    assertEquals(2L, invoicesCount);
    assertNotNull(actual.getUpdatedAt());
    assertEquals(
        PreSignedLink.builder()
            .value(PRE_SIGNED_URL)
            .expirationDelay((int) DEFAULT_EXPIRATION_DELAY)
            .updatedAt(actual.getUpdatedAt())
            .build(),
        actual);
  }
}
