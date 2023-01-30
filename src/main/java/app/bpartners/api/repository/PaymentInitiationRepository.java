package app.bpartners.api.repository;

import app.bpartners.api.model.PaymentInitiation;
import app.bpartners.api.model.PaymentRedirection;
import java.util.List;

public interface PaymentInitiationRepository {
  List<PaymentRedirection> saveAll(List<PaymentInitiation> paymentInitiation, String invoice);
}
