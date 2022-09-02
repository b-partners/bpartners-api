package app.bpartners.api.repository.fintecture.implementation;

import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.repository.fintecture.FintectureConf;
import app.bpartners.api.repository.fintecture.FintecturePaymentReqRepository;
import app.bpartners.api.repository.fintecture.model.PaymentInitiation;
import app.bpartners.api.repository.fintecture.model.PaymentRedirection;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;


@Repository
@AllArgsConstructor
public class FintecturePaymentReqRepositoryImpl implements FintecturePaymentReqRepository {
  private final FintectureConf fintectureConf;
  private final ProjectTokenManager tokenManager;

  @Override
  public PaymentRedirection generatePaymentUrl(
      PaymentInitiation paymentReq, String redirectUri) {
    return null;
  }
}
