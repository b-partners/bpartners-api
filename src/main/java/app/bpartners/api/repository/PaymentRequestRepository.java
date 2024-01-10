package app.bpartners.api.repository;

import app.bpartners.api.model.PaymentRequest;
import java.util.List;

public interface PaymentRequestRepository {
  List<PaymentRequest> saveAll(List<PaymentRequest> payments);

  List<PaymentRequest> findAllByReference(String reference);
}
