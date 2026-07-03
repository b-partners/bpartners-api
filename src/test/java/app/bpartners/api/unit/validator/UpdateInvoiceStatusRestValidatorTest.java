package app.bpartners.api.unit.validator;

import static app.bpartners.api.endpoint.rest.model.InvoiceStatus.PAID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.bpartners.api.endpoint.rest.model.UpdateInvoiceStatus;
import app.bpartners.api.endpoint.rest.validator.UpdateInvoiceStatusRestValidator;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.List;
import org.junit.jupiter.api.Test;

class UpdateInvoiceStatusRestValidatorTest {
  UpdateInvoiceStatusRestValidator subject = new UpdateInvoiceStatusRestValidator();

  @Test
  void missing_invoice_status_ko() {
    var actual =
        assertThrows(
            BadRequestException.class,
            () -> subject.accept(new UpdateInvoiceStatus().invoiceIdentifier("invoiceId")));

    assertEquals("InvoiceStatus is mandatory. ", actual.getMessage());
  }

  @Test
  void both_identifier_and_reference_provided_ko() {
    var actual =
        assertThrows(
            BadRequestException.class,
            () ->
                subject.accept(
                    new UpdateInvoiceStatus()
                        .invoiceIdentifier("invoiceId")
                        .invoiceReference("invoiceRef")
                        .invoiceStatus(PAID)));

    assertEquals(
        "Both invoiceIdentifier and invoiceReference can not be provided at same time.",
        actual.getMessage());
  }

  @Test
  void only_identifier_provided_ok() {
    assertDoesNotThrow(
        () ->
            subject.accept(
                new UpdateInvoiceStatus().invoiceIdentifier("invoiceId").invoiceStatus(PAID)));
  }

  @Test
  void only_reference_provided_ok() {
    assertDoesNotThrow(
        () ->
            subject.accept(
                new UpdateInvoiceStatus().invoiceReference("invoiceRef").invoiceStatus(PAID)));
  }

  @Test
  void accept_list_delegates_to_each_element() {
    var valid = new UpdateInvoiceStatus().invoiceIdentifier("invoiceId").invoiceStatus(PAID);
    var invalid = new UpdateInvoiceStatus();

    var actual =
        assertThrows(BadRequestException.class, () -> subject.accept(List.of(valid, invalid)));

    assertEquals("InvoiceStatus is mandatory. ", actual.getMessage());
  }
}
