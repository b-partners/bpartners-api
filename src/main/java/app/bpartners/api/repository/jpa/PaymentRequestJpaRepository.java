package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HPaymentRequest;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRequestJpaRepository extends JpaRepository<HPaymentRequest, String> {

  List<HPaymentRequest> findByAccountId(String accountId, Pageable pageable);

  List<HPaymentRequest> findByIdInvoice(String idInvoice);

  void deleteAllByIdInvoice(String idInvoice);
}