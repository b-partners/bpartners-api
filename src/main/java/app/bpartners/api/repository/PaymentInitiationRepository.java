package app.bpartners.api.repository;

import app.bpartners.api.endpoint.rest.model.InvoiceStatus;
import app.bpartners.api.model.PaymentInitiation;
import app.bpartners.api.model.PaymentRedirection;
import java.util.List;

public interface PaymentInitiationRepository {
  List<PaymentRedirection> saveAll(List<PaymentInitiation> paymentInitiation);

  void saveAll(List<PaymentInitiation> paymentInitiation, String invoice, InvoiceStatus status);

}
