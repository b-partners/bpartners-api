package app.bpartners.api.repository.fintecture;

import app.bpartners.api.repository.fintecture.schema.PaymentInitiation;
import app.bpartners.api.repository.fintecture.schema.PaymentRedirection;

public interface FintecturePaymentReqRepository {
  PaymentRedirection generatePaymentUrl(PaymentInitiation paymentReq, String redirectUri);
}
