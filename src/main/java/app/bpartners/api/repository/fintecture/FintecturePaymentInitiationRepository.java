package app.bpartners.api.repository.fintecture;

import app.bpartners.api.repository.fintecture.model.FPaymentInitiation;
import app.bpartners.api.repository.fintecture.model.FPaymentRedirection;

public interface FintecturePaymentInitiationRepository {
  FPaymentRedirection save(FPaymentInitiation paymentInitiation, String redirectUri);
}
