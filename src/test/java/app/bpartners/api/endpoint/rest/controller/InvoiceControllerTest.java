package app.bpartners.api.endpoint.rest.controller;

import static app.bpartners.api.endpoint.rest.model.ArchiveStatus.ENABLED;
import static app.bpartners.api.endpoint.rest.model.InvoiceExportOutputFormat.ZIP;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;
import static app.bpartners.api.endpoint.rest.security.model.Role.ADMIN_ROLE;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.rest.mapper.InvoiceExportRequestRestMapper;
import app.bpartners.api.endpoint.rest.mapper.InvoiceRestMapper;
import app.bpartners.api.endpoint.rest.mapper.InvoicesSummaryRestMapper;
import app.bpartners.api.endpoint.rest.model.InvoiceExportRequest;
import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.endpoint.rest.validator.InvoiceReferenceValidator;
import app.bpartners.api.endpoint.rest.validator.UpdateInvoiceStatusRestValidator;
import app.bpartners.api.endpoint.rest.validator.UpdatePaymentRegValidator;
import app.bpartners.api.file.bucket.BucketComponent;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.ForbiddenException;
import app.bpartners.api.service.invoice.InvoiceExportRequestService;
import app.bpartners.api.service.invoice.InvoiceService;
import app.bpartners.api.service.invoice.InvoiceSummaryService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

class InvoiceControllerTest {
  InvoiceRestMapper invoiceRestMapperMock = mock();
  InvoiceService invoiceServiceMock = mock();
  InvoiceReferenceValidator invoiceReferenceValidatorMock = mock();
  UpdatePaymentRegValidator updatePaymentRegValidatorMock = mock();
  InvoicesSummaryRestMapper invoicesSummaryRestMapperMock = mock();
  InvoiceSummaryService invoiceSummaryServiceMock = mock();
  InvoiceExportRequestService invoiceExportRequestServiceMock = mock();
  BucketComponent bucketComponentMock = mock();
  InvoiceExportRequestRestMapper invoiceExportRequestRestMapper =
      new InvoiceExportRequestRestMapper(bucketComponentMock);
  UpdateInvoiceStatusRestValidator updateInvoiceStatusRestValidatorMock = mock();

  InvoiceController subject =
      new InvoiceController(
          invoiceRestMapperMock,
          invoiceServiceMock,
          invoiceReferenceValidatorMock,
          updatePaymentRegValidatorMock,
          invoicesSummaryRestMapperMock,
          invoiceSummaryServiceMock,
          invoiceExportRequestServiceMock,
          invoiceExportRequestRestMapper,
          updateInvoiceStatusRestValidatorMock);

  @Test
  void get_invoice_export_request_when_own_request() {
    var userIdentifier = randomUUID().toString();
    var requestId = randomUUID().toString();
    var from = LocalDate.of(2026, 1, 1);
    var to = LocalDate.of(2026, 1, 31);

    var userMock = mock(User.class);
    MockedStatic<AuthProvider> authProviderMockedStatic = mockStatic(AuthProvider.class);
    authProviderMockedStatic.when(AuthProvider::getAuthenticatedUser).thenReturn(userMock);
    when(userMock.getId()).thenReturn(userIdentifier);
    when(invoiceExportRequestServiceMock.getById(requestId))
        .thenReturn(
            app.bpartners.api.model.InvoiceExportRequest.builder()
                .id(requestId)
                .userId(userIdentifier)
                .batchList(List.of())
                .from(from)
                .to(to)
                .archiveStatus(ENABLED)
                .statusList(List.of(CONFIRMED, PAID))
                .outputFormat(ZIP)
                .creationDatetime(now())
                .build());
    when(bucketComponentMock.presign(any(), any())).thenReturn(null);

    var actual = subject.retrieveInvoiceExportRequestById(userIdentifier, requestId);

    assertEquals(
        new InvoiceExportRequest()
            .id(requestId)
            .from(from)
            .to(to)
            .statusList(List.of(CONFIRMED, PAID))
            .archiveStatus(ENABLED)
            .outputFormat(ZIP)
            .batchList(List.of()),
        actual);

    authProviderMockedStatic.close();
  }

  @Test
  void get_invoice_export_request_when_admin() {
    var userIdentifier = randomUUID().toString();
    var requestId = randomUUID().toString();
    var from = LocalDate.of(2026, 1, 1);
    var to = LocalDate.of(2026, 1, 31);

    var userMock = mock(User.class);
    MockedStatic<AuthProvider> authProviderMockedStatic = mockStatic(AuthProvider.class);
    authProviderMockedStatic.when(AuthProvider::getAuthenticatedUser).thenReturn(userMock);
    when(userMock.getId()).thenReturn(randomUUID().toString());
    when(userMock.getRoles()).thenReturn(List.of(ADMIN_ROLE));
    when(invoiceExportRequestServiceMock.getById(requestId))
        .thenReturn(
            app.bpartners.api.model.InvoiceExportRequest.builder()
                .id(requestId)
                .userId(userIdentifier)
                .batchList(List.of())
                .from(from)
                .to(to)
                .archiveStatus(ENABLED)
                .statusList(List.of(CONFIRMED, PAID))
                .outputFormat(ZIP)
                .creationDatetime(now())
                .build());
    when(bucketComponentMock.presign(any(), any())).thenReturn(null);

    var actual = subject.retrieveInvoiceExportRequestById(userIdentifier, requestId);

    assertEquals(
        new InvoiceExportRequest()
            .id(requestId)
            .from(from)
            .to(to)
            .statusList(List.of(CONFIRMED, PAID))
            .archiveStatus(ENABLED)
            .outputFormat(ZIP)
            .batchList(List.of()),
        actual);

    authProviderMockedStatic.close();
  }

  @Test
  void throw_forbidden_exception_when_not_admin_and_not_own_request() {
    var userIdentifier = randomUUID().toString();
    var requestId = randomUUID().toString();
    var userMock = mock(User.class);
    MockedStatic<AuthProvider> authProviderMockedStatic = mockStatic(AuthProvider.class);
    authProviderMockedStatic.when(AuthProvider::getAuthenticatedUser).thenReturn(userMock);
    when(userMock.getId()).thenReturn(randomUUID().toString());
    when(userMock.getRoles()).thenReturn(List.of());

    var actualException =
        assertThrows(
            ForbiddenException.class,
            () -> subject.retrieveInvoiceExportRequestById(userIdentifier, requestId));

    assertEquals("User can only export their own invoices", actualException.getMessage());

    authProviderMockedStatic.close();
  }

  @Test
  void request_invoice_statuses_update_validates_then_delegates_to_service() {
    var accountId = randomUUID().toString();
    var userIdentifier = randomUUID().toString();
    var restUpdateInvoiceStatus =
        new app.bpartners.api.endpoint.rest.model.UpdateInvoiceStatus()
            .invoiceIdentifier("invoiceId")
            .invoiceStatus(InvoiceStatus.PAID);
    var updateInvoiceStatuses = List.of(restUpdateInvoiceStatus);

    MockedStatic<AuthProvider> authProviderMockedStatic = mockStatic(AuthProvider.class);
    authProviderMockedStatic.when(AuthProvider::getAuthenticatedUserId).thenReturn(userIdentifier);
    doNothing().when(updateInvoiceStatusRestValidatorMock).accept(updateInvoiceStatuses);

    subject.requestInvoiceStatusesUpdate(accountId, updateInvoiceStatuses);

    verify(updateInvoiceStatusRestValidatorMock).accept(updateInvoiceStatuses);
    ArgumentCaptor<List<app.bpartners.api.model.UpdateInvoiceStatus>> captor =
        ArgumentCaptor.forClass(List.class);
    verify(invoiceServiceMock).requestInvoiceStatusesUpdate(captor.capture());
    var actual = captor.getValue();
    assertEquals(1, actual.size());
    assertEquals(
        new app.bpartners.api.model.UpdateInvoiceStatus(
            "invoiceId", null, InvoiceStatus.PAID, userIdentifier),
        actual.get(0));

    authProviderMockedStatic.close();
  }
}
