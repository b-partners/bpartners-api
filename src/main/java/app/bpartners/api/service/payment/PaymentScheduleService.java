package app.bpartners.api.service.payment;

import app.bpartners.api.repository.jpa.model.HPaymentRequest;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class PaymentScheduleService {
  public static final String PAYMENT_CREATED = "payment_created";

  public static String paymentMessage(List<HPaymentRequest> paymentRequests) {
    StringBuilder builder = new StringBuilder();
    for (HPaymentRequest payment : paymentRequests) {
      builder
          .append("(id=")
          .append(payment.getId())
          .append(", sessionId=")
          .append(payment.getSessionId())
          .append(", status=")
          .append(payment.getStatus())
          .append(")")
          .append(" ");
    }
    if (!builder.toString().isEmpty()) {
      return builder.toString();
    }
    return null;
  }
}
