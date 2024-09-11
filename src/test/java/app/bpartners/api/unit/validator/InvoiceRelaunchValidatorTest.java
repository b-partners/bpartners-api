package app.bpartners.api.unit.validator;

import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.validator.InvoiceRelaunchValidator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InvoiceRelaunchValidatorTest {
    InvoiceRelaunchValidator subject = new InvoiceRelaunchValidator();

    @Test
    void subject_throws_bad_request_exception() {
        var invoice = Invoice.builder().id("id").status(InvoiceStatus.DRAFT).build();

        var actual = assertThrows(BadRequestException.class, () -> {
            subject.accept(invoice);
        });

        var expected = "Invoice."
                + invoice.getId()
                + " actual status is "
                + invoice.getStatus()
                + " and it cannot be relaunched";
        assertEquals(expected, actual.getMessage());
    }
}
