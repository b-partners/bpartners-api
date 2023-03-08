package app.bpartners.api.repository;

import app.bpartners.api.model.PaymentInitiation;
import app.bpartners.api.model.PaymentRedirection;
import app.bpartners.api.repository.jpa.model.HPaymentRequest;
import java.util.List;

public interface PaymentInitiationRepository {
  List<PaymentRedirection> saveAll(List<PaymentInitiation> paymentInitiation, String invoice);

  List<HPaymentRequest> retrievePaymentEntitiesWithUrl(
      List<PaymentInitiation> paymentInitiation, String invoice);

  List<HPaymentRequest> retrievePaymentEntities(
      List<PaymentInitiation> paymentInitiation, String invoice);
}
