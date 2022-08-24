package app.bpartners.api.repository.fintecture;

import app.bpartners.api.repository.fintecture.schema.PaymentReq;
import app.bpartners.api.repository.fintecture.schema.PaymentUrl;

public interface FintecturePaymentReqRepository {
  PaymentUrl generatePaymentUrl(PaymentReq paymentReq, String redirectUri);
}
