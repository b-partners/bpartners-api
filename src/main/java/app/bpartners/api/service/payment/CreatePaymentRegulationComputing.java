package app.bpartners.api.service.payment;

import static java.util.UUID.randomUUID;

import app.bpartners.api.model.CreatePaymentRegulation;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.PaymentRequest;
import java.util.List;
import java.util.function.Function;
import org.springframework.stereotype.Component;

@Component
public class CreatePaymentRegulationComputing
    implements Function<Invoice, List<CreatePaymentRegulation>> {
  @Override
  public List<CreatePaymentRegulation> apply(Invoice invoice) {
    List<CreatePaymentRegulation> paymentReg = invoice.getPaymentRegulations();
    paymentReg.forEach(
        payment -> {
          PaymentRequest request = payment.getPaymentRequest();
          request.setId(String.valueOf(randomUUID()));
          request.setExternalId(null);
          request.setPaymentUrl(null);
        });
    return paymentReg;
  }
}
