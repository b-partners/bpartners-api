package app.bpartners.api.repository.fintecture;

import app.bpartners.api.repository.fintecture.model.PaymentInitiation;
import app.bpartners.api.repository.fintecture.model.PaymentRedirection;

public interface FintecturePaymentReqRepository {
  PaymentRedirection generatePaymentUrl(PaymentInitiation paymentReq, String redirectUri);
}
