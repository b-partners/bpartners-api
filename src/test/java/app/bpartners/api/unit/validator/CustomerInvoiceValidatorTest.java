package app.bpartners.api.unit.validator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.repository.CustomerRepository;
import app.bpartners.api.service.invoice.CustomerInvoiceValidator;
import org.junit.jupiter.api.Test;

class CustomerInvoiceValidatorTest {
  CustomerRepository repository;
  CustomerInvoiceValidator subject = new CustomerInvoiceValidator(repository);

  @Test
  void subject_throws_bad_request_exception() {
    var invoice = Invoice.builder().build();

    var actual =
        assertThrows(
            BadRequestException.class,
            () -> {
              subject.accept(invoice);
            });

    var expected = "Invoice.customer is mandatory";
    assertEquals(expected, actual.getMessage());
  }
}
