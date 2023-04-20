package app.bpartners.api.repository.fintecture;

import app.bpartners.api.repository.fintecture.model.PaymentMeta;
import app.bpartners.api.repository.fintecture.model.Session;
import java.util.List;

public interface FintecturePaymentInfoRepository {
  Session getPaymentBySessionId(String sessionId);

  List<Session> getAllPayments();

  Session cancelPayment(PaymentMeta requestBody, String sessionId);
}
