package app.bpartners.api.service.payment;

import static app.bpartners.api.service.utils.PaymentUtils.computeTotalPriceFromPaymentReq;

import app.bpartners.api.model.CreatePaymentRegulation;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.PaymentRequest;
import app.bpartners.api.model.mapper.PaymentRequestMapper;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.apfloat.Aprational;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class CreatePaymentRegulationComputing
    implements Function<Invoice, List<CreatePaymentRegulation>> {
  private final PaymentRegulationService prs;
  private final PaymentInitiationComputing paymentInitiationComputing;
  private final PaymentRequestMapper requestMapper;

  @Override
  public List<CreatePaymentRegulation> apply(Invoice actual) {
    var paymentInitiations = paymentInitiationComputing.apply(actual);
    var paymentRequests =
        prs.retrievePaymentEntities(paymentInitiations, actual.getId(), actual.getStatus());
    return convertPaymentRequests(paymentRequests);
  }

  private List<CreatePaymentRegulation> convertPaymentRequests(
      List<PaymentRequest> paymentRequests) {
    Fraction totalPrice = computeTotalPriceFromPaymentReq(paymentRequests);
    return paymentRequests.stream()
        .map(
            payment -> {
              Fraction percent =
                  totalPrice.getCentsRoundUp() == 0
                      ? new Fraction()
                      : payment.getAmount().operate(totalPrice, Aprational::divide);
              return requestMapper.toPaymentRegulation(payment, percent);
            })
        .collect(Collectors.toList());
  }
}
