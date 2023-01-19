package app.bpartners.api.repository.fintecture.implementation;

import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.repository.fintecture.FintectureConf;
import app.bpartners.api.repository.fintecture.FintecturePaymentInitiationRepository;
import app.bpartners.api.repository.fintecture.model.PaymentInitiation;
import app.bpartners.api.repository.fintecture.model.PaymentRedirection;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.NoSuchAlgorithmException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import static app.bpartners.api.endpoint.rest.security.swan.SwanConf.BEARER_PREFIX;
import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.api.repository.fintecture.implementation.utils.FintecturePaymentUtils.ACCEPT;
import static app.bpartners.api.repository.fintecture.implementation.utils.FintecturePaymentUtils.APPLICATION_JSON;
import static app.bpartners.api.repository.fintecture.implementation.utils.FintecturePaymentUtils.AUTHORIZATION;
import static app.bpartners.api.repository.fintecture.implementation.utils.FintecturePaymentUtils.DATE;
import static app.bpartners.api.repository.fintecture.implementation.utils.FintecturePaymentUtils.LANGUAGE;
import static app.bpartners.api.repository.fintecture.implementation.utils.FintecturePaymentUtils.REQUEST_ID;
import static app.bpartners.api.repository.fintecture.implementation.utils.FintecturePaymentUtils.SIGNATURE;
import static app.bpartners.api.repository.fintecture.implementation.utils.FintecturePaymentUtils.getDigest;
import static app.bpartners.api.repository.fintecture.implementation.utils.FintecturePaymentUtils.getHeaderSignatureWithDigest;
import static app.bpartners.api.repository.fintecture.implementation.utils.FintecturePaymentUtils.getParsedDate;
import static java.util.UUID.randomUUID;

@Slf4j
@Repository
public class FintecturePaymentInitiationRepositoryImpl implements
    FintecturePaymentInitiationRepository {
  private final FintectureConf fintectureConf;
  private final ProjectTokenManager tokenManager;
  private HttpClient httpClient;

  public FintecturePaymentInitiationRepositoryImpl(FintectureConf fintectureConf,
                                                   ProjectTokenManager tokenManager) {
    this.fintectureConf = fintectureConf;
    this.tokenManager = tokenManager;
    httpClient = HttpClient.newBuilder().build();
  }

  public FintecturePaymentInitiationRepositoryImpl httpClient(HttpClient httpClient) {
    this.httpClient = httpClient;
    return this;
  }

  @Override
  public PaymentRedirection save(PaymentInitiation paymentReq, String redirectUri) {
    try {
      String urlParams = String.format("?redirectUri=%s&state=12341234", redirectUri);
      String data = new ObjectMapper().writeValueAsString(paymentReq);
      String requestId = String.valueOf(randomUUID());
      String digest = getDigest(data);
      String date = getParsedDate();
      HttpRequest request = HttpRequest.newBuilder()
          .uri(new URI(fintectureConf.getRequestToPayUrl() + urlParams))
          .header("Content-Type", APPLICATION_JSON)
          .header(ACCEPT, APPLICATION_JSON)
          .header(REQUEST_ID, requestId)
          .header(LANGUAGE, "fr")
          .header("digest", digest)
          .header(DATE, date)
          .header(SIGNATURE,
              getHeaderSignatureWithDigest(fintectureConf, requestId, digest, date, urlParams))
          .header(AUTHORIZATION, BEARER_PREFIX + tokenManager.getFintectureProjectToken())
          .POST(HttpRequest.BodyPublishers.ofString(data))
          .build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      PaymentRedirection redirectionResponse = new ObjectMapper()
          .findAndRegisterModules() //Load DateTime Module
          .readValue(response.body(), PaymentRedirection.class);
      if (redirectionResponse.getMeta() == null
          || redirectionResponse.getMeta().getStatus() != 200
          && redirectionResponse.getMeta().getStatus() != 201) {
        log.warn("Error from Fintecture occured={}", response.body());
        return null;
      }
      return redirectionResponse;
    } catch (IOException | URISyntaxException | NoSuchAlgorithmException e) {
      throw new ApiException(ApiException.ExceptionType.SERVER_EXCEPTION, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }
}
