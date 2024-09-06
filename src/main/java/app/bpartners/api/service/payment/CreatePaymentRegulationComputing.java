package app.bpartners.api.service.payment;

import static app.bpartners.api.service.utils.PaymentUtils.computeTotalPriceFromPaymentReq;

import app.bpartners.api.model.CreatePaymentRegulation;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.Invoice;
import app.bpartners.api.model.PaymentRequest;
import app.bpartners.api.model.mapper.PaymentRequestMapper;
import app.bpartners.api.service.PaymentInitiationService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.apfloat.Aprational;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class CreatePaymentRegulationComputing {
  private final PaymentInitiationService pis;
  private final PaymentInitiationComputing paymentInitiationComputing;
  private final PaymentRequestMapper requestMapper;

  public List<CreatePaymentRegulation> computeWithPisURL(Invoice actual) {
    var paymentInitiations = paymentInitiationComputing.apply(actual);
    var paymentRequests =
        pis.retrievePaymentEntitiesWithUrl(paymentInitiations, actual.getId(), actual.getUser());
    return convertPaymentRequests(paymentRequests);
  }

  public List<CreatePaymentRegulation> computeWithoutPisURL(Invoice actual) {
    var paymentInitiations = paymentInitiationComputing.apply(actual);
    var paymentRequests =
        pis.retrievePaymentEntities(paymentInitiations, actual.getId(), actual.getStatus());
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
