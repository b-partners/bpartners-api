package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HPaymentRequest;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRequestJpaRepository extends JpaRepository<HPaymentRequest, String> {
}