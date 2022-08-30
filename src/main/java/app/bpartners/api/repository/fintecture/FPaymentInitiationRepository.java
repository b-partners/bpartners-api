package app.bpartners.api.repository.fintecture;

import app.bpartners.api.repository.fintecture.model.PaymentInitiation;
import app.bpartners.api.repository.fintecture.model.PaymentRedirection;

public interface FPaymentInitiationRepository {
  PaymentRedirection save(PaymentInitiation paymentReq, String redirectUri);
}
