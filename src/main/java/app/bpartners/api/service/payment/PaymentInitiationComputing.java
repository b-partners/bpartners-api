package app.bpartners.api.service.payment;

import static app.bpartners.api.endpoint.rest.model.EnableStatus.ENABLED;
import static java.util.UUID.randomUUID;

import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.PaymentInitiation;
import app.bpartners.api.model.mapper.PaymentRequestMapper;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class PaymentInitiationComputing implements Function<Invoice, List<PaymentInitiation>> {
  private final PaymentRequestMapper requestMapper;

  @Override
  public List<PaymentInitiation> apply(Invoice invoice) {
    var payments =
        invoice.getPaymentRegulations().stream()
            .map(
                payment -> {
                  var randomId = String.valueOf(randomUUID());
                  var paymentRequest = payment.getPaymentRequest();
                  var label = paymentRequest.getLabel();
                  var reference =
                      paymentRequest.getReference() == null
                          ? invoice.getRealReference()
                          : paymentRequest.getReference();

                  paymentRequest.setExternalId(randomId); // TODO: seems bad
                  paymentRequest.setEnableStatus(ENABLED); // TODO: seems bad

                  return requestMapper.convertFromInvoice(
                      randomId,
                      label,
                      reference,
                      invoice,
                      payment,
                      paymentRequest.getPaymentHistoryStatus());
                })
            .sorted(Comparator.comparing(PaymentInitiation::getPaymentDueDate))
            .toList();

    var counter = new AtomicInteger(1);
    payments.stream()
        .filter(payment -> payment.getLabel() == null)
        .forEach(
            payment -> {
              var label =
                  (counter.get() == payments.size())
                      ? invoice.getTitle() + " - Restant dû"
                      : invoice.getTitle() + " - Acompte N°" + counter.get();
              payment.setLabel(label);
              counter.incrementAndGet();
            });
    return payments;
  }
}
