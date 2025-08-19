package app.bpartners.api.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import app.bpartners.api.model.CreatePaymentRegulation;
import app.bpartners.api.model.PaymentRequest;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.service.payment.PaymentService;
import java.util.List;
import org.junit.jupiter.api.Test;

class PaymentServiceTest {
  PaymentService subject = new PaymentService();

  @Test
  void filter_by_payment_id_ok() {
    var paymentId = "paymentId";
    var invoiceId = "invoiceId";
    var paymentRequest = PaymentRequest.builder().id(paymentId).build();
    var paymentRegulation =
        CreatePaymentRegulation.builder().paymentRequest(paymentRequest).build();

    var actual = subject.filterByPaymentId(paymentId, invoiceId, List.of(paymentRegulation));

    assertEquals(paymentRequest, actual);
  }

  @Test
  void filter_by_payment_id_ko() {
    var paymentId = "paymentId";
    var invoiceId = "invoiceId";
    var paymentRequest = PaymentRequest.builder().id("otherPaymentId").build();
    var paymentRegulation =
        CreatePaymentRegulation.builder().paymentRequest(paymentRequest).build();

    var actual =
        assertThrows(
                NotFoundException.class,
                () -> subject.filterByPaymentId(paymentId, invoiceId, List.of(paymentRegulation)))
            .getMessage();

    var expected =
        "Invoice(id=" + invoiceId + ") " + "does not contain PaymentRequest(id=" + paymentId + ")";
    assertEquals(expected, actual);
  }
}
