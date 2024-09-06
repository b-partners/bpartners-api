package app.bpartners.api.service.payment;

import app.bpartners.api.model.CreatePaymentRegulation;
import app.bpartners.api.model.PaymentRequest;
import app.bpartners.api.model.exception.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

  public PaymentRequest filterByPaymentId(
      String paymentId, String invoiceId, List<CreatePaymentRegulation> paymentRegulations) {
    return paymentRegulations.stream()
        .filter(payment -> payment.getPaymentRequest().getId().equals(paymentId))
        .findAny()
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Invoice(id="
                        + invoiceId
                        + ") "
                        + "does not contain PaymentRequest(id="
                        + paymentId
                        + ")"))
        .getPaymentRequest();
  }
}
