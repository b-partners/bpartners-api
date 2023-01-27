package app.bpartners.api.unit.validator;

import app.bpartners.api.endpoint.rest.model.CrupdateInvoice;
import app.bpartners.api.endpoint.rest.validator.CreateProductValidator;
import app.bpartners.api.endpoint.rest.validator.CrupdateInvoiceValidator;
import app.bpartners.api.endpoint.rest.validator.PaymentRegValidator;
import app.bpartners.api.model.exception.BadRequestException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PROPOSAL;
import static app.bpartners.api.integration.conf.TestUtils.createProduct4;
import static app.bpartners.api.integration.conf.TestUtils.createProduct5;
import static app.bpartners.api.integration.conf.TestUtils.customer1;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class CrupdateInvoiceValidatorTest {

  private final CrupdateInvoiceValidator validator =
      new CrupdateInvoiceValidator(
          mock(PaymentRegValidator.class),
          mock(CreateProductValidator.class));

  CrupdateInvoice validInvoice() {
    return new CrupdateInvoice()
        .ref("BP004")
        .title("Facture sans produit")
        .customer(customer1())
        .products(List.of(createProduct4(), createProduct5()))
        .status(PROPOSAL)
        .sendingDate(LocalDate.now())
        .toPayAt(LocalDate.now().plusDays(1L));
  }

  CrupdateInvoice invalidInvoice() {
    return new CrupdateInvoice()
        .ref(null)
        .title(null)
        .customer(customer1())
        .products(List.of(createProduct4(), createProduct5()))
        .status(PROPOSAL)
        .sendingDate(LocalDate.of(2022, 10, 12))
        .toPayAt(LocalDate.of(2022, 10, 13));
  }

  @Test
  void valid_invoice() {
    CrupdateInvoice invoice = validInvoice();
    assertDoesNotThrow(() -> validator.accept(invoice));
  }

  @Test
  void invalid_invoice() {
    CrupdateInvoice invoice = new CrupdateInvoice().status(null);
    assertThrows(BadRequestException.class,
        () -> validator.accept(invoice));
  }

}
