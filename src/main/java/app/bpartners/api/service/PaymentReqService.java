package app.bpartners.api.service;

import app.bpartners.api.model.PaymentInitiation;
import app.bpartners.api.model.PaymentRedirection;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.repository.PaymentReqRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PaymentReqService {
  private final PaymentReqRepository repository;

  public List<PaymentRedirection> createPaymentReq(List<PaymentInitiation> paymentReqs) {
    if (paymentReqs.size() > 1) {
      throw new NotImplementedException("Only one payment request is supported.");
    }
    PaymentInitiation paymentReq = paymentReqs.get(0);
    return repository.generatePaymentUrl(paymentReq);
  }
}
