package app.bpartners.api.repository;

import app.bpartners.api.model.PaymentInitiation;
import app.bpartners.api.model.PaymentRedirection;
import java.util.List;

public interface PaymentReqRepository {
  //TODO(repository-functions)
  List<PaymentRedirection> generatePaymentUrl(PaymentInitiation paymentReq);
}
