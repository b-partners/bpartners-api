package app.bpartners.api.repository.fintecture.implementation;

import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.repository.fintecture.FintectureConf;
import app.bpartners.api.repository.fintecture.FintecturePaymentInfoRepository;
import app.bpartners.api.repository.fintecture.model.PaymentMeta;
import app.bpartners.api.repository.fintecture.model.Session;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;
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
import static app.bpartners.api.repository.fintecture.implementation.utils.FintecturePaymentUtils.getHeaderSignature;
import static app.bpartners.api.repository.fintecture.implementation.utils.FintecturePaymentUtils.getHeaderSignatureWithDigest;
import static app.bpartners.api.repository.fintecture.implementation.utils.FintecturePaymentUtils.getParsedDate;
import static java.util.UUID.randomUUID;

@Slf4j
@Repository
public class FintecturePaymentInfoRepositoryImpl implements FintecturePaymentInfoRepository {
  private final FintectureConf fintectureConf;
  private final ProjectTokenManager tokenManager;
  private final ObjectMapper objectMapper = new ObjectMapper();
  private HttpClient httpClient;

  public FintecturePaymentInfoRepositoryImpl(FintectureConf fintectureConf,
                                             ProjectTokenManager tokenManager) {
    this.fintectureConf = fintectureConf;
    this.tokenManager = tokenManager;
    httpClient = HttpClient.newBuilder().build();
  }

  public FintecturePaymentInfoRepositoryImpl httpClient(HttpClient httpClient) {
    this.httpClient = httpClient;
    return this;
  }

  @Override
  public Session getPaymentBySessionId(String sessionId) {
    try {
      String urlParams = String.format("/%s", sessionId);
      String requestId = String.valueOf(randomUUID());
      String date = getParsedDate();
      HttpRequest request = HttpRequest.newBuilder()
          .uri(new URI(fintectureConf.getPaymentUrl() + urlParams))
          .header(ACCEPT, APPLICATION_JSON)
          .header(REQUEST_ID, requestId)
          .header(LANGUAGE, "fr")
          .header(DATE, date)
          .header(SIGNATURE, getHeaderSignature(fintectureConf, requestId, date, urlParams))
          .header(AUTHORIZATION, BEARER_PREFIX + tokenManager.getFintectureProjectToken())
          .GET().build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      Session sessionResponse = objectMapper
          .findAndRegisterModules()
          .readValue(response.body(), Session.class);
      if (sessionResponse.getMeta() == null
          || !Objects.equals(sessionResponse.getMeta().getCode(), "200")) {
        log.warn("Error from Fintecture occured={}", response.body());
        return null;
      }
      return sessionResponse;
    } catch (IOException | URISyntaxException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  @Override
  public Session cancelPayment(PaymentMeta requestBody, String sessionId) {
    try {
      String urlParams = String.format("/%s", sessionId);
      String data = new ObjectMapper().writeValueAsString(requestBody);
      String requestId = String.valueOf(randomUUID());
      String digest = getDigest(data);
      String date = getParsedDate();
      HttpRequest request = HttpRequest.newBuilder()
          .uri(new URI(fintectureConf.getPaymentUrl() + urlParams))
          .header("Content-Type", "application/json")
          .header(ACCEPT, APPLICATION_JSON)
          .header(REQUEST_ID, requestId)
          .header(LANGUAGE, "fr")
          .header("digest", digest)
          .header(DATE, date)
          .header(SIGNATURE,
              getHeaderSignatureWithDigest(fintectureConf, requestId, digest, date, urlParams))
          .header(AUTHORIZATION, BEARER_PREFIX + tokenManager.getFintectureProjectToken())
          .method("PATCH", HttpRequest.BodyPublishers.ofString(data))
          .build();
      HttpResponse<String> response =
          httpClient.send(request, HttpResponse.BodyHandlers.ofString());
      return objectMapper
          .findAndRegisterModules()
          .readValue(response.body(), Session.class);
    } catch (IOException | URISyntaxException | NoSuchAlgorithmException e) {
      throw new ApiException(SERVER_EXCEPTION, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }
}