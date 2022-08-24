package app.bpartners.api.service;

import app.bpartners.api.model.PaymentReq;
import app.bpartners.api.model.PaymentUrl;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.repository.PaymentReqRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class PaymentReqService {
  private final PaymentReqRepository repository;

  public List<PaymentUrl> createPaymentReq(List<PaymentReq> paymentReqs) {
    if (paymentReqs.size() > 1) {
      throw new NotImplementedException("Only one payment request is supported.");
    }
    PaymentReq paymentReq = paymentReqs.get(0);
    return repository.generatePaymentUrl(paymentReq);
  }
}
