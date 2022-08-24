package app.bpartners.api.repository.fintecture.implementation;

import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.repository.fintecture.FintectureConf;
import app.bpartners.api.repository.fintecture.FintecturePaymentReqRepository;
import app.bpartners.api.repository.fintecture.schema.PaymentReq;
import app.bpartners.api.repository.fintecture.schema.PaymentUrl;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

import static app.bpartners.api.endpoint.rest.security.swan.SwanConf.BEARER_PREFIX;

@Repository
@AllArgsConstructor
public class FintecturePaymentReqRepositoryImpl implements FintecturePaymentReqRepository {
  private final FintectureConf fintectureConf;
  private final ProjectTokenManager tokenManager;

  @Override
  public PaymentUrl generatePaymentUrl(PaymentReq paymentReq, String redirectUri) {
    try {
      HttpClient httpClient = HttpClient.newBuilder().build();
      String urlParams = String.format("?redirectUri=%s&state=1234", redirectUri);
      String data = new ObjectMapper().writeValueAsString(paymentReq);
      HttpRequest request = HttpRequest.newBuilder()
          .uri(new URI(fintectureConf.getConnectPISUrl() + urlParams))
          .header("Content-Type", "application/json")
          .header("Accept", "application/json")
          .header("Authorization", BEARER_PREFIX + tokenManager.getFintectureProjectToken())
          .POST(HttpRequest.BodyPublishers.ofString(data))
          .build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      return new ObjectMapper()
          .findAndRegisterModules() //Load DateTime Module
          .readValue(response.body(), PaymentUrl.class);
    } catch (IOException | InterruptedException | URISyntaxException e) {
      throw new ApiException(ApiException.ExceptionType.SERVER_EXCEPTION, e);
    }
  }
}
