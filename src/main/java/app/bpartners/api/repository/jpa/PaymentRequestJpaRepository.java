package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HPaymentRequest;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRequestJpaRepository extends JpaRepository<HPaymentRequest, String> {
  Optional<HPaymentRequest> findBySessionId(String sessionId);

  List<HPaymentRequest> findByAccountId(String accountId, Pageable pageable);
}