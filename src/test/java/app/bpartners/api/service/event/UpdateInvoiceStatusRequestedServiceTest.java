package app.bpartners.api.service.event;

import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.DRAFT;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.event.model.UpdateInvoiceStatusRequested;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.service.invoice.InvoiceService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class UpdateInvoiceStatusRequestedServiceTest {
  InvoiceService invoiceServiceMock = mock(InvoiceService.class);
  InvoiceRepository invoiceRepositoryMock = mock(InvoiceRepository.class);
  UpdateInvoiceStatusRequestedService subject =
      new UpdateInvoiceStatusRequestedService(invoiceServiceMock, invoiceRepositoryMock);

  @Test
  void updates_invoice_found_by_reference() {
    var invoice = Invoice.builder().id("invoiceId").ref("invoiceRef").status(DRAFT).build();
    when(invoiceRepositoryMock.findByIdUserAndRef("userId", "invoiceRef"))
        .thenReturn(List.of(invoice));

    subject.accept(new UpdateInvoiceStatusRequested(null, "invoiceRef", PAID, "userId"));

    verify(invoiceRepositoryMock, never()).pwFindOptionalById(any());
    ArgumentCaptor<Invoice> captor = ArgumentCaptor.forClass(Invoice.class);
    verify(invoiceServiceMock).crupdateInvoice(captor.capture());
    assertEquals(PAID, captor.getValue().getStatus());
    assertEquals("invoiceId", captor.getValue().getId());
  }

  @Test
  void updates_invoice_found_by_identifier_when_not_found_by_reference() {
    var invoice = Invoice.builder().id("invoiceId").ref("invoiceRef").status(DRAFT).build();
    when(invoiceRepositoryMock.findByIdUserAndRef("userId", "invoiceRef")).thenReturn(List.of());
    when(invoiceRepositoryMock.pwFindOptionalById("invoiceId")).thenReturn(Optional.of(invoice));

    subject.accept(new UpdateInvoiceStatusRequested("invoiceId", "invoiceRef", PAID, "userId"));

    ArgumentCaptor<Invoice> captor = ArgumentCaptor.forClass(Invoice.class);
    verify(invoiceServiceMock).crupdateInvoice(captor.capture());
    assertEquals(PAID, captor.getValue().getStatus());
    assertEquals("invoiceId", captor.getValue().getId());
  }

  @Test
  void does_nothing_when_invoice_not_found_by_reference_nor_identifier() {
    when(invoiceRepositoryMock.findByIdUserAndRef("userId", "invoiceRef")).thenReturn(List.of());
    when(invoiceRepositoryMock.pwFindOptionalById("invoiceId")).thenReturn(Optional.empty());

    subject.accept(new UpdateInvoiceStatusRequested("invoiceId", "invoiceRef", PAID, "userId"));

    verify(invoiceServiceMock, never()).crupdateInvoice(any());
  }
}
