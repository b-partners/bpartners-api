package app.bpartners.api.unit.service;

import app.bpartners.api.endpoint.event.model.gen.FileUploaded;
import app.bpartners.api.endpoint.rest.model.FileType;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.service.FileService;
import app.bpartners.api.service.FileUploadedService;
import app.bpartners.api.service.InvoiceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static app.bpartners.api.endpoint.rest.model.FileType.INVOICE;
import static app.bpartners.api.integration.conf.TestUtils.FILE_ID;
import static app.bpartners.api.integration.conf.TestUtils.INVOICE1_ID;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FileUploadedServiceTest {
  FileUploadedService fileUploadedService;
  FileService fileService;
  InvoiceService invoiceService;

  @BeforeEach
  void setUp() {
    fileService = mock(FileService.class);
    invoiceService = mock(InvoiceService.class);
    fileUploadedService = new FileUploadedService(fileService, invoiceService);

    doNothing().when(fileService).uploadFile(any(), any(), any(), any());
    when(invoiceService.persistFileId(any())).thenReturn(Invoice.builder().build());
  }

  @Test
  void persistFileId_trigger_with_invoiceId() {
    String fileId = "fileId.jpg";
    FileType fileType = INVOICE;
    String accountId = JOE_DOE_ACCOUNT_ID;
    String invoiceId = INVOICE1_ID;
    when(invoiceService.persistFileId(invoiceId)).thenReturn(
        Invoice.builder().fileId(fileId).build());

    fileUploadedService.accept(FileUploaded.builder()
        .fileId(null)
        .fileType(fileType)
        .accountId(accountId)
        .invoiceId(invoiceId)
        .fileAsBytes(null)
        .build());

    verify(fileService, times(1)).saveChecksum(fileId, fileType, accountId, null);
    verify(invoiceService, times(1)).persistFileId(invoiceId);
  }

  @Test
  void persistFileId_trigger_without_invoiceId() {
    String fileId = FILE_ID;
    FileType fileType = INVOICE;
    String accountId = JOE_DOE_ACCOUNT_ID;

    fileUploadedService.accept(FileUploaded.builder()
        .fileId(fileId)
        .fileType(fileType)
        .accountId(accountId)
        .invoiceId(null)
        .fileAsBytes(null)
        .build());

    verify(fileService, times(1)).saveChecksum(fileId, fileType, accountId, null);
    verify(invoiceService, times(0)).persistFileId(any());
  }
}
