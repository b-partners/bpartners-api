package app.bpartners.api.repository.jpa;

import app.bpartners.api.endpoint.rest.model.PaymentStatus;
import app.bpartners.api.repository.jpa.model.HPaymentRequest;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRequestJpaRepository extends JpaRepository<HPaymentRequest, String> {
  List<HPaymentRequest> findAllByIdInvoice(String idInvoice);

  Optional<HPaymentRequest> findBySessionId(String sessionId);

  List<HPaymentRequest> findAllByStatus(PaymentStatus status);

  List<HPaymentRequest> findAllByReferenceContainingIgnoreCase(String reference);

  void deleteAllByIdInvoice(String idInvoice);
}
