package app.bpartners.api.unit;

import static app.bpartners.api.endpoint.rest.model.PaymentMethod.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import app.bpartners.api.model.Invoice;
import org.junit.jupiter.api.Test;

class InvoiceTest {

  @Test
  void valid_payment_method_value() {
    var actualNull = Invoice.builder().paymentMethod(null).build();
    var actualMultiple = Invoice.builder().paymentMethod(MULTIPLE).build();
    var actualCash = Invoice.builder().paymentMethod(CASH).build();
    var actualBankTransfer = Invoice.builder().paymentMethod(BANK_TRANSFER).build();
    var actualCheque = Invoice.builder().paymentMethod(CHEQUE).build();
    var actualCreditCard = Invoice.builder().paymentMethod(CREDIT_CARD).build();
    var actualUnknown = Invoice.builder().paymentMethod(UNKNOWN).build();

    assertNull(actualNull.getPaymentMethodValue());
    assertEquals("ESPÈCES", actualCash.getPaymentMethodValue());
    assertEquals("VIREMENT BANCAIRE", actualBankTransfer.getPaymentMethodValue());
    assertEquals("CHÈQUE", actualCheque.getPaymentMethodValue());
    assertEquals("CARTE DE CRÉDIT", actualCreditCard.getPaymentMethodValue());
    assertEquals("INCONNU", actualUnknown.getPaymentMethodValue());
    assertEquals("PLUSIEURS MÉTHODES", actualMultiple.getPaymentMethodValue());
  }
}
