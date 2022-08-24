package app.bpartners.api.repository;

import app.bpartners.api.model.PaymentReq;
import app.bpartners.api.model.PaymentUrl;
import java.util.List;

public interface PaymentReqRepository {
  List<PaymentUrl> generatePaymentUrl(PaymentReq paymentReq);
}
