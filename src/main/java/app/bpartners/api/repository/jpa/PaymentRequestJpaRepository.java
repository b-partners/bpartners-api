package app.bpartners.api.repository.jpa;

import app.bpartners.api.endpoint.rest.model.PaymentStatus;
import app.bpartners.api.repository.jpa.model.HPaymentRequest;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRequestJpaRepository extends JpaRepository<HPaymentRequest, String> {
  List<HPaymentRequest> findAllByStatus(PaymentStatus status);

  void deleteAllByIdInvoice(String idInvoice);
}