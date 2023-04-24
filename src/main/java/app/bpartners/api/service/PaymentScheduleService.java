package app.bpartners.api.service;

import app.bpartners.api.repository.fintecture.FintecturePaymentInfoRepository;
import app.bpartners.api.repository.fintecture.model.Session;
import app.bpartners.api.repository.jpa.PaymentRequestJpaRepository;
import app.bpartners.api.repository.jpa.model.HPaymentRequest;
import java.util.List;
import javax.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static app.bpartners.api.endpoint.rest.model.PaymentStatus.PAID;
import static app.bpartners.api.endpoint.rest.model.PaymentStatus.UNPAID;

@Service
@AllArgsConstructor
@Slf4j
public class PaymentScheduleService {
  public static final String PAYMENT_CREATED = "payment_created";
  private final FintecturePaymentInfoRepository infoRepository;
  private final PaymentRequestJpaRepository jpaRepository;

  //TODO: remove when payment status are correctly set
  @PostConstruct
  public void updatePaymentStatus() {
    List<HPaymentRequest> unpaidPayments = jpaRepository.findAllByStatus(UNPAID);
    List<Session> externalPayments = infoRepository.getAllPayments();
    for (HPaymentRequest payment : unpaidPayments) {
      for (Session externalPayment : externalPayments) {
        if (payment.getSessionId().equals(externalPayment.getMeta().getSessionId())
            && externalPayment.getMeta().getStatus().equals(PAYMENT_CREATED)) {
          payment.setStatus(PAID);
          break;
        }
      }
    }
    List<HPaymentRequest> paidPayments = jpaRepository.saveAll(unpaidPayments);
    log.info("Payment requests " + paymentMessage(paidPayments) + " updated successfully");
  }

  String paymentMessage(List<HPaymentRequest> paymentRequests) {
    StringBuilder builder = new StringBuilder();
    for (HPaymentRequest payment : paymentRequests) {
      builder.append("(id=")
          .append(payment.getId())
          .append(", sessionId=")
          .append(payment.getSessionId())
          .append(", status=")
          .append(payment.getStatus())
          .append(")")
          .append(", ");
    }
    if (!builder.toString().isEmpty()) {
      return builder.toString();
    }
    return null;
  }
}
