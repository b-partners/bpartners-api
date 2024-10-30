package app.bpartners.api.unit.service;

import static app.bpartners.api.endpoint.rest.model.ArchiveStatus.ENABLED;
import static app.bpartners.api.endpoint.rest.model.FileType.INVOICE;
import static app.bpartners.api.file.FileHashAlgorithm.SHA256;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.event.model.InvoiceExportLinkRequested;
import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.file.FileHash;
import app.bpartners.api.file.FileZipper;
import app.bpartners.api.mail.Mailer;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.User;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.service.aws.S3Service;
import app.bpartners.api.service.event.InvoiceExportLinkRequestedService;
import app.bpartners.api.service.utils.TemplateResolverEngine;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.zip.ZipFile;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class InvoiceExportLinkRequestedServiceTest {
  private static final String USER_ID = "userId";
  private static final String ACCOUNT_ID = "accountId";
  private static final String PRE_SIGNED_URL = "preSignedURL";

  InvoiceRepository repositoryMock = mock();
  S3Service s3ServiceMock = mock();
  FileZipper fileZipper = new FileZipper();
  Mailer mailerMock = mock();
  UserRepository userRepositoryMock = mock();
  TemplateResolverEngine templateResolverEngine = new TemplateResolverEngine();
  InvoiceExportLinkRequestedService subject =
      new InvoiceExportLinkRequestedService(
          fileZipper,
          mailerMock,
          userRepositoryMock,
          repositoryMock,
          s3ServiceMock,
          templateResolverEngine);

  @BeforeEach
  @SneakyThrows
  void setUp() {
    when(userRepositoryMock.getByIdAccount(any())).thenReturn(actualUser());
    when(s3ServiceMock.uploadFile(any(), any(), any(), any()))
        .thenReturn(new FileHash(SHA256, "fileHash"));
    when(s3ServiceMock.presignURL(any(), any(), any(), any())).thenReturn(PRE_SIGNED_URL);
    when(s3ServiceMock.downloadFile(any(), any(), any()))
        .thenReturn(File.createTempFile(randomUUID().toString(), randomUUID().toString()));
    doNothing().when(mailerMock).accept(any());
  }

  private AccountHolder defaultAccountHolder() {
    return AccountHolder.builder().name("").email("exemple@gmail.com").build();
  }

  private User actualUser() {
    return User.builder().id(USER_ID).accountHolders(List.of(defaultAccountHolder())).build();
  }

  @Test
  void generate_export_link_with_empty_invoices_ok() {
    when(repositoryMock.findAllByIdUserAndCriteria(
            any(), anyList(), any(), anyList(), anyInt(), anyInt()))
        .thenReturn(List.of());
    LocalDate today = LocalDate.now();
    List<InvoiceStatus> providedStatuses = List.of();

    subject.accept(
        InvoiceExportLinkRequested.builder()
            .accountId(ACCOUNT_ID)
            .providedStatuses(providedStatuses)
            .providedArchiveStatus(ENABLED)
            .providedFrom(today)
            .providedTo(today.plusDays(1L))
            .build());

    var fileCaptor = ArgumentCaptor.forClass(File.class);
    verify(s3ServiceMock).uploadFile(any(), any(), any(), fileCaptor.capture());
    Long invoicesCount;
    try (var invoiceZipFile = new ZipFile(fileCaptor.getValue())) {
      invoicesCount = invoiceZipFile.stream().count();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    assertEquals(0L, invoicesCount);
  }

  private File crupdateFile(File file) {
    if (!file.exists()) {
      try {
        boolean fileCreated = file.createNewFile();
        if (!fileCreated) {
          return file;
        }
      } catch (IOException e) {
        e.printStackTrace();
      }
    }
    return file;
  }

  @Test
  void generate_export_link_with_invoices_ok() {
    LocalDate today = LocalDate.now();
    var file1 =
        crupdateFile(Paths.get("src", "test", "resources", "files", "REFinvoiceId1.pdf").toFile());
    when(s3ServiceMock.downloadFile(INVOICE, "invoiceFileId1", USER_ID)).thenReturn(file1);
    var file2 =
        crupdateFile(Paths.get("src", "test", "resources", "files", "REFinvoiceId2.pdf").toFile());
    when(s3ServiceMock.downloadFile(INVOICE, "invoiceFileId2", USER_ID)).thenReturn(file2);
    when(repositoryMock.findAllByIdUserAndCriteria(
            any(), anyList(), any(), anyList(), anyInt(), anyInt()))
        .thenReturn(
            List.of(
                Invoice.builder()
                    .id("invoiceId1")
                    .fileId("invoiceFileId1")
                    .ref("REFinvoiceId1")
                    .sendingDate(today)
                    .user(actualUser())
                    .build(),
                Invoice.builder()
                    .id("invoiceId2")
                    .fileId("invoiceFileId2")
                    .ref("REFinvoiceId2")
                    .sendingDate(today)
                    .user(actualUser())
                    .build()));
    List<InvoiceStatus> providedStatuses = List.of();

    subject.accept(
        InvoiceExportLinkRequested.builder()
            .accountId(ACCOUNT_ID)
            .providedStatuses(providedStatuses)
            .providedArchiveStatus(ENABLED)
            .providedFrom(today)
            .providedTo(today.plusDays(1L))
            .build());

    var fileCaptor = ArgumentCaptor.forClass(File.class);
    verify(s3ServiceMock).uploadFile(any(), any(), any(), fileCaptor.capture());
    Long invoicesCount;
    try (var invoiceZipFile = new ZipFile(fileCaptor.getValue())) {
      invoicesCount = invoiceZipFile.stream().count();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    assertEquals(2L, invoicesCount);
  }
}
