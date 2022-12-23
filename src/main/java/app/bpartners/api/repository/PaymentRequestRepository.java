package app.bpartners.api.repository;

import app.bpartners.api.model.PaymentRequest;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;

public interface PaymentRequestRepository {
  List<PaymentRequest> findByAccountId(String accountId, Pageable pageable);

  Optional<PaymentRequest> findByInvoiceId(String invoiceId);
}
