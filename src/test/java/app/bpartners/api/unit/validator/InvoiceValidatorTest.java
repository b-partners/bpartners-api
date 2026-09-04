package app.bpartners.api.unit.validator;

import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.CONFIRMED;
import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.repository.InvoiceRepository;
import app.bpartners.api.service.invoice.InvoiceValidator;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InvoiceValidatorTest {
  private static final String USER_ID = "user_id";
  private static final String REFERENCE = "REF-01092026121742";
  InvoiceRepository invoiceRepositoryMock = mock();
  InvoiceValidator subject = new InvoiceValidator(invoiceRepositoryMock);

  @BeforeEach
  void setUp() {
    when(invoiceRepositoryMock.findByIdUserAndRef(anyString(), any())).thenReturn(List.of());
  }

  @Test
  void paid_invoice_with_unused_reference_is_available() {
    assertDoesNotThrow(() -> subject.checkReferenceAvailability(invoice("invoice_id", PAID)));
  }

  @Test
  void paid_invoice_keeping_its_own_reference_is_available() {
    when(invoiceRepositoryMock.findByIdUserAndRef(USER_ID, REFERENCE))
        .thenReturn(List.of(invoice("invoice_id", PAID)));

    assertDoesNotThrow(() -> subject.checkReferenceAvailability(invoice("invoice_id", PAID)));
  }

  @Test
  void paid_invoice_confirmed_by_another_invoice_is_available() {
    when(invoiceRepositoryMock.findByIdUserAndRef(USER_ID, REFERENCE))
        .thenReturn(List.of(invoice("other_invoice_id", CONFIRMED)));

    assertDoesNotThrow(() -> subject.checkReferenceAvailability(invoice("invoice_id", PAID)));
  }

  @Test
  void paid_invoice_reusing_another_paid_reference_is_rejected() {
    when(invoiceRepositoryMock.findByIdUserAndRef(USER_ID, REFERENCE))
        .thenReturn(List.of(invoice("other_invoice_id", PAID)));

    var actual =
        assertThrows(
            BadRequestException.class,
            () -> subject.checkReferenceAvailability(invoice("invoice_id", PAID)));

    assertEquals("La référence " + REFERENCE + " est déjà utilisée", actual.getMessage());
  }

  private Invoice invoice(String identifier, InvoiceStatus status) {
    return Invoice.builder()
        .id(identifier)
        .ref(REFERENCE)
        .status(status)
        .user(User.builder().id(USER_ID).build())
        .build();
  }
}
