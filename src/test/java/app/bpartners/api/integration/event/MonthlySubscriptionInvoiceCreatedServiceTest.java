package app.bpartners.api.integration.event;

import static app.bpartners.api.service.utils.MonthUtils.actualMonthValue;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.event.model.MonthlySubscriptionInvoiceCreated;
import app.bpartners.api.file.FileWriter;
import app.bpartners.api.model.Customer;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.User;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.service.aws.S3Service;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.event.MonthlySubscriptionInvoiceCreatedService;
import java.io.File;
import java.time.Year;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

class MonthlySubscriptionInvoiceCreatedServiceTest {
  InvoiceRepository invoiceRepositoryMock = mock();
  SesService mailerMock = mock();
  S3Service s3ServiceMock = mock();
  FileWriter fileWriterMock = mock();
  MonthlySubscriptionInvoiceCreatedService subject =
      new MonthlySubscriptionInvoiceCreatedService(
          invoiceRepositoryMock, mailerMock, s3ServiceMock, fileWriterMock);

  @SneakyThrows
  @Test
  void download_invoice_and_notify_email() {
    var invoiceId = randomUUID().toString();
    var invoiceMock = mock(Invoice.class);
    var userMock = mock(User.class);
    var fileMock = mock(File.class);
    var customerMock = mock(Customer.class);
    var attachmentAsBytes = new byte[0];
    var userId = "userId";
    var customerEmail = "customerEmail";
    var fileId = "fileId";
    var invoiceTitle =
        "[BPartners] Votre facture du mois de "
            + actualMonthValue()
            + " "
            + Year.now().getValue()
            + " est disponible";
    var ccExpected = "tech@bpartners.app";
    var htmlBodyExpected = "";
    when(userMock.getId()).thenReturn(userId);
    when(customerMock.getEmail()).thenReturn(customerEmail);
    when(invoiceMock.getFileId()).thenReturn(fileId);
    when(invoiceMock.getUser()).thenReturn(userMock);
    when(invoiceMock.getCustomer()).thenReturn(customerMock);
    when(invoiceMock.getTitle()).thenReturn(invoiceTitle);
    when(invoiceRepositoryMock.findById(invoiceId)).thenReturn(invoiceMock);
    when(s3ServiceMock.downloadFile(any(), any(), any())).thenReturn(fileMock);
    when(fileWriterMock.writeAsByte(fileMock)).thenReturn(attachmentAsBytes);

    assertDoesNotThrow(
        () ->
            subject.accept(
                MonthlySubscriptionInvoiceCreated.builder().invoiceId(invoiceId).build()));
    verify(mailerMock)
        .sendEmail(
            eq(customerEmail), eq(ccExpected), eq(invoiceTitle), eq(htmlBodyExpected), any());
  }
}
