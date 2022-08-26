package app.bpartners.api.repository.fintecture.implementation;

import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.repository.api.fintecture.FintectureApi;
import app.bpartners.api.repository.fintecture.FintectureConf;
import app.bpartners.api.repository.fintecture.FintecturePaymentReqRepository;
import app.bpartners.api.repository.fintecture.schema.PaymentReq;
import app.bpartners.api.repository.fintecture.schema.PaymentUrl;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

import java.io.IOException;


@Repository
@AllArgsConstructor
public class FintecturePaymentReqRepositoryImpl implements FintecturePaymentReqRepository {
  private final FintectureConf fintectureConf;
  private final ProjectTokenManager tokenManager;

  @Override
  public PaymentUrl generatePaymentUrl(PaymentReq paymentReq, String redirectUri) {
    try {
      FintectureApi api = new FintectureApi();
      return api.generatePaymentUrl(paymentReq).getData();

    } catch (IOException | app.bpartners.api.endpoint.rest.client.ApiException e) {
      throw new ApiException(ApiException.ExceptionType.SERVER_EXCEPTION, e);
    }
  }
}
