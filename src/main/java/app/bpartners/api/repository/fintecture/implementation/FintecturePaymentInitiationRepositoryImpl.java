package app.bpartners.api.repository.fintecture.implementation;

import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.repository.fintecture.FintectureConf;
import app.bpartners.api.repository.fintecture.FintecturePaymentInitiationRepository;
import app.bpartners.api.repository.fintecture.model.FPaymentInitiation;
import app.bpartners.api.repository.fintecture.model.FPaymentRedirection;
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
import static app.bpartners.api.service.utils.SecurityUtils.BEARER_PREFIX;
import static java.util.UUID.randomUUID;
import static org.apache.tika.metadata.HttpHeaders.CONTENT_TYPE;

@Slf4j
@Repository
public class FintecturePaymentInitiationRepositoryImpl implements
    FintecturePaymentInitiationRepository {
  private final FintectureConf fintectureConf;
  private final ProjectTokenManager tokenManager;
  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
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
  public FPaymentRedirection save(FPaymentInitiation paymentInitiation, String redirectUri) {
    String globalPayload = null;
    HttpRequest globalRequest = null;
    try {
      String urlParams = String.format("?redirectUri=%s&state=12341234", redirectUri);
      String payload = objectMapper.writeValueAsString(paymentInitiation);
      globalPayload = payload;
      String requestId = String.valueOf(randomUUID());
      String digest = getDigest(payload);
      String date = getParsedDate();
      var request = HttpRequest.newBuilder()
          .uri(new URI(fintectureConf.getRequestToPayUrl() + urlParams))
          .header(CONTENT_TYPE, APPLICATION_JSON)
          .header(ACCEPT, APPLICATION_JSON)
          .header(REQUEST_ID, requestId)
          .header(LANGUAGE, "fr")
          .header("digest", digest)
          .header(DATE, date)
          .header(SIGNATURE,
              getHeaderSignatureWithDigest(fintectureConf, requestId, digest, date, urlParams))
          .header(AUTHORIZATION, bearerToken())
          .POST(HttpRequest.BodyPublishers.ofString(payload))
          .build();
      globalRequest = request;
      return sendPaymentInitRequest(request);
    } catch (IOException e) {
      log.warn(
          "[Fintecture] Payment was not initiated because of {}."
              + " Payload was {}", e.getMessage(), globalPayload);
      try {
        return sendPaymentInitRequest(globalRequest);
      } catch (IOException | InterruptedException ex) {
        log.warn("[Fintecture] Second retry failed, payload was {}", globalRequest);
        throw new ApiException(ApiException.ExceptionType.SERVER_EXCEPTION, ex);
      }
    } catch (URISyntaxException | NoSuchAlgorithmException e) {
      log.warn("[Fintecture] Payment was not initiated. Payload was {}", globalPayload);
      throw new ApiException(ApiException.ExceptionType.SERVER_EXCEPTION, e);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ApiException(SERVER_EXCEPTION, e);
    }
  }

  private FPaymentRedirection sendPaymentInitRequest(HttpRequest request)
      throws IOException, InterruptedException {
    var response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    var redirectionObj = objectMapper.readValue(response.body(), FPaymentRedirection.class);
    if (redirectionObj.getMeta() == null
        || redirectionObj.getMeta().getStatus() != 200
        && redirectionObj.getMeta().getStatus() != 201) {
      log.warn("Error from Fintecture : {}", response.body());
      return null;
    }
    return redirectionObj;
  }

  private String bearerToken() {
    return BEARER_PREFIX + tokenManager.getFintectureProjectToken();
  }
}
