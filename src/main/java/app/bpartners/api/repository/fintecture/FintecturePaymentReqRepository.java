package app.bpartners.api.repository.fintecture;

import app.bpartners.api.repository.fintecture.schema.PaymentUrl;
import app.bpartners.api.repository.fintecture.schema.PaymentReq;

public interface FintecturePaymentReqRepository {
  PaymentUrl generatePaymentUrl(PaymentReq paymentReq, String redirectUri);
}
