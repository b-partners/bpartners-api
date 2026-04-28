package app.bpartners.api.unit;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.model.InvoiceExportRequest;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.repository.jpa.InvoiceExportRequestRepository;
import app.bpartners.api.service.invoice.InvoiceExportRequestService;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class InvoiceExportRequestServiceTest {
  InvoiceExportRequestRepository invoiceExportRequestRepositoryMock = mock();
  InvoiceExportRequestService subject =
      new InvoiceExportRequestService(invoiceExportRequestRepositoryMock);

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
}
