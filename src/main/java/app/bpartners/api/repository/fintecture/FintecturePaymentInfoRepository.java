package app.bpartners.api.repository.fintecture;

import app.bpartners.api.repository.fintecture.model.PaymentMeta;
import app.bpartners.api.repository.fintecture.model.Session;

public interface FintecturePaymentInfoRepository {
  Session getPaymentBySessionId(String sessionId);

  Session cancelPayment(PaymentMeta requestBody, String sessionId);
}
