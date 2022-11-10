package app.bpartners.api.event;

import app.bpartners.api.endpoint.event.EventServiceInvoker;
import app.bpartners.api.endpoint.event.model.TypedFileSaved;
import app.bpartners.api.endpoint.event.model.TypedInvoiceRelaunchSaved;
import app.bpartners.api.endpoint.event.model.gen.FileSaved;
import app.bpartners.api.endpoint.event.model.gen.InvoiceRelaunchSaved;
import app.bpartners.api.endpoint.rest.model.FileType;
import app.bpartners.api.service.FileSavedService;
import app.bpartners.api.service.InvoiceCrupdatedService;
import app.bpartners.api.service.InvoiceRelaunchSavedService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_ID;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class EventServiceInvokerTest {
  EventServiceInvoker eventServiceInvoker;
  InvoiceRelaunchSavedService invoiceRelaunchSavedService;
  FileSavedService fileSavedService;
  InvoiceCrupdatedService invoiceCrupdatedService;

  @BeforeEach
  void setUp() {
    invoiceRelaunchSavedService = mock(InvoiceRelaunchSavedService.class);
    fileSavedService = mock(FileSavedService.class);
    invoiceCrupdatedService = mock(InvoiceCrupdatedService.class);
    eventServiceInvoker =
        new EventServiceInvoker(invoiceRelaunchSavedService, fileSavedService,
            invoiceCrupdatedService);
  }

  @Test
  void invoiceRelaunchSaved_invokes_corresponding_service() {
    TypedInvoiceRelaunchSaved emailSent =
        new TypedInvoiceRelaunchSaved(InvoiceRelaunchSaved.builder()
            .subject(null)
            .recipient(null)
            .htmlBody(null)
            .attachmentName(null)
            .invoice(null)
            .accountHolder(null)
            .logoFileId(null)
            .build());
    eventServiceInvoker.accept(emailSent);

    verify(invoiceRelaunchSavedService, times(1)).accept(
        (InvoiceRelaunchSaved) emailSent.getPayload());
  }

  @Test
  void fileUploaded_invokes_corresponding_service() {
    TypedFileSaved fileUploaded = new TypedFileSaved(FileSaved.builder()
        .fileId(null)
        .fileType(FileType.INVOICE)
        .fileAsBytes(null)
        .accountId(JOE_DOE_ACCOUNT_ID)
        .build());
    eventServiceInvoker.accept(fileUploaded);

    verify(fileSavedService, times(1)).accept((FileSaved) fileUploaded.getPayload());
  }
}