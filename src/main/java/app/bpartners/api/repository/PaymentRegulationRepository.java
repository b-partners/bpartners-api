package app.bpartners.api.repository;

import app.bpartners.api.model.PaymentRegulation;
import app.bpartners.api.repository.jpa.model.HPaymentRequest;
import java.util.List;

public interface PaymentRegulationRepository {
  List<HPaymentRequest> retrievePaymentEntities(
      List<PaymentRegulation> paymentRegulation, String invoice);
}
