package app.bpartners.api.unit;

import static app.bpartners.api.endpoint.rest.model.ArchiveStatus.ENABLED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;
import static app.bpartners.api.service.invoice.InvoiceExportRequestService.DEFAULT_BATCH_SIZE;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.InvoiceExportRequested;
import app.bpartners.api.model.InvoiceExportRequest;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.repository.jpa.InvoiceExportRequestRepository;
import app.bpartners.api.service.invoice.InvoiceExportRequestService;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class InvoiceExportRequestServiceTest {
  private static final String USER_ID = randomUUID().toString();
  private static final LocalDate FROM = LocalDate.of(2026, 1, 1);
  private static final LocalDate TO = LocalDate.of(2026, 1, 31);

  InvoiceExportRequestRepository invoiceExportRequestRepositoryMock = mock();
  InvoiceRepository invoiceRepositoryMock = mock();
  EventProducer<InvoiceExportRequested> eventProducerMock = mock();
  InvoiceExportRequestService subject =
      new InvoiceExportRequestService(
          invoiceExportRequestRepositoryMock, invoiceRepositoryMock, eventProducerMock);

  @Test
  void throws_exception_when_no_invoice_export_request_found() {
    var requestIdentifier = randomUUID().toString();
    when(invoiceExportRequestRepositoryMock.findById(requestIdentifier))
        .thenReturn(Optional.empty());

    var actual = assertThrows(NotFoundException.class, () -> subject.getById(requestIdentifier));

    assertEquals(
        "InvoiceExportRequest.id=" + requestIdentifier + " not found", actual.getMessage());
  }

  @Test
  void get_invoice_export_request_service_by_id() {
    var requestIdentifier = randomUUID().toString();
    var expected = new InvoiceExportRequest();
    when(invoiceExportRequestRepositoryMock.findById(requestIdentifier))
        .thenReturn(Optional.of(expected));

    var actual = subject.getById(requestIdentifier);

    assertEquals(expected, actual);
  }

  @Test
  void create_invoice_export_request_plans_batches_and_fires_first_page() {
    givenInvoiceCountIs(1_200);
    givenRepositorySavesAsIs();

    var actual =
        subject.createInvoiceExportRequestList(List.of(requestBuilder().batchSize(500).build()));

    var saved = actual.getFirst();
    assertNotNull(saved.getId());
    assertEquals(1_200, saved.getTotalInvoiceCount());
    assertEquals(3, saved.getTotalBatchCount());
    assertEquals(500, saved.getBatchSize());
    assertTrue(saved.getBatchList().isEmpty());
    assertEquals(
        List.of(
            InvoiceExportRequested.builder().invoiceExportRequestId(saved.getId()).page(0).build()),
        capturedEvents());
  }

  @Test
  void create_invoice_export_request_keeps_provided_id() {
    var providedId = randomUUID().toString();
    givenInvoiceCountIs(1);
    givenRepositorySavesAsIs();

    var actual =
        subject.createInvoiceExportRequestList(List.of(requestBuilder().id(providedId).build()));

    assertEquals(providedId, actual.getFirst().getId());
  }

  @Test
  void create_invoice_export_request_does_not_fire_event_when_no_invoice() {
    givenInvoiceCountIs(0);
    givenRepositorySavesAsIs();

    var actual = subject.createInvoiceExportRequestList(List.of(requestBuilder().build()));

    assertEquals(0, actual.getFirst().getTotalBatchCount());
    verify(eventProducerMock, never()).accept(any());
  }

  @Test
  void create_invoice_export_request_counts_only_requested_statuses() {
    givenInvoiceCountIs(3);
    givenRepositorySavesAsIs();

    subject.createInvoiceExportRequestList(List.of(requestBuilder().build()));

    verify(invoiceRepositoryMock)
        .countAllByIdUserAndSendingDateBetweenAndCriteria(
            eq(USER_ID), eq(FROM), eq(TO), eq(List.of(CONFIRMED, PAID)), eq(ENABLED));
  }

  private InvoiceExportRequest.InvoiceExportRequestBuilder requestBuilder() {
    return InvoiceExportRequest.builder()
        .id(randomUUID().toString())
        .userId(USER_ID)
        .from(FROM)
        .to(TO)
        .archiveStatus(ENABLED)
        .statusList(List.of(CONFIRMED, PAID))
        .batchSize(DEFAULT_BATCH_SIZE);
  }

  private void givenInvoiceCountIs(int count) {
    when(invoiceRepositoryMock.countAllByIdUserAndSendingDateBetweenAndCriteria(
            any(), any(), any(), any(), any()))
        .thenReturn(count);
  }

  private void givenRepositorySavesAsIs() {
    when(invoiceExportRequestRepositoryMock.saveAll(any()))
        .thenAnswer(invocation -> List.copyOf(invocation.getArgument(0)));
  }

  private Collection<InvoiceExportRequested> capturedEvents() {
    ArgumentCaptor<Collection<InvoiceExportRequested>> captor =
        ArgumentCaptor.forClass(Collection.class);
    verify(eventProducerMock).accept(captor.capture());
    return captor.getValue();
  }
}
