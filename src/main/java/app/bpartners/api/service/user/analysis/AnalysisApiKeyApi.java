package app.bpartners.api.service.user.analysis;

import static app.bpartners.api.service.user.analysis.ConsumerType.INSURANCE;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.POST;

import app.bpartners.api.model.User;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class AnalysisApiKeyApi {
  private static final ConsumerType DEFAULT_CONSUMER_TYPE = INSURANCE;
  private static final Double DEFAULT_MAX_SURFACE = 0.0;
  private static final List<DetectableObjectModel> DEFAULT_ALLOWED_MODELS =
      List.of(DetectableObjectModel.ofName("BP_TOITURE"));

  private static final List<AuthorizedZone> DEFAULT_AUTHORIZED_ZONES = List.of(); // deprecated
  private static final List<DetectableObjectType> DEFAULT_DETECTABLE_OBJECT_TYPES =
      List.of(); // deprecated
  private static final String AUTHORIZATION_HEADER = "x-api-key";
  private static final String API_KEY_API_PATH = "/api/keys";

  private final String geoJobsBaseUrl;
  private final String geoJobsAdminApiKey;
  private final RestTemplate restTemplate;

  public AnalysisApiKeyApi(
      @Value("${geo.jobs.base.url}") String geoJobsBaseUrl,
      @Value("${geo.jobs.admin.api.key}") String geoJobsAdminApiKey,
      RestTemplate restTemplate) {
    this.geoJobsBaseUrl = geoJobsBaseUrl;
    this.geoJobsAdminApiKey = geoJobsAdminApiKey;
    this.restTemplate = restTemplate;
  }

  public @NotNull List<CreatedAnalysisApiKey> createAnalysisApiKeys(User user) {
    var uriString = getAnalysisApiKeyApiUri();

    var headers = new HttpHeaders();
    headers.add(AUTHORIZATION_HEADER, geoJobsAdminApiKey);

    var request = new HttpEntity<>(List.of(toAnalysisApiKeyCreation(user)), headers);

    var response =
        restTemplate.exchange(
            uriString,
            POST,
            request,
            new ParameterizedTypeReference<List<CreatedAnalysisApiKey>>() {});

    if (!response.getStatusCode().is2xxSuccessful()) {
      throw new RuntimeException(
          "API exception occurred while attempting to create user.email="
              + user.getEmail()
              + " analysis api key");
    }

    if (response.getBody() != null && response.getBody().isEmpty()) {
      throw new RuntimeException(
          "API failed to create user.email=" + user.getEmail() + " analysis api key");
    }

    return response.getBody();
  }

  public @NotNull RevokedAnalysisApiKey requestAnalysisApiKeyRevocation(String apiKeyToRevoke) {
    var uriString = getAnalysisApiKeyApiUri();

    var headers = new HttpHeaders();
    headers.add(AUTHORIZATION_HEADER, geoJobsAdminApiKey);

    var request = new HttpEntity<>(new AnalysisApiKeyRevocation(apiKeyToRevoke), headers);

    var response =
        restTemplate.exchange(
            uriString, DELETE, request, new ParameterizedTypeReference<RevokedAnalysisApiKey>() {});

    if (!response.getStatusCode().is2xxSuccessful()) {
      throw new RuntimeException(
          "API exception occurred while attempting to revoke analysis api key "
              + hide(apiKeyToRevoke));
    }

    if (response.getBody() == null) {
      throw new RuntimeException("API failed to provide revoked key" + hide(apiKeyToRevoke));
    }

    return response.getBody();
  }

  private @NotNull String getAnalysisApiKeyApiUri() {
    try {
      UriComponentsBuilder uriBuilder =
          UriComponentsBuilder.fromUri(new URI(geoJobsBaseUrl + API_KEY_API_PATH));
      return uriBuilder.toUriString();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  private AnalysisApiKeyCreation toAnalysisApiKeyCreation(User user) {
    return new AnalysisApiKeyCreation(
        user.getName(),
        user.getEmail(),
        DEFAULT_CONSUMER_TYPE,
        DEFAULT_MAX_SURFACE,
        DEFAULT_ALLOWED_MODELS,
        DEFAULT_AUTHORIZED_ZONES,
        DEFAULT_DETECTABLE_OBJECT_TYPES);
  }

  static String hide(String apiKey) {
    int keyLength = apiKey.length();
    int hideRange = keyLength / (keyLength / 6);
    String shownPart = apiKey.substring(hideRange, (keyLength - hideRange));
    String hider = "*".repeat(hideRange);

    return hider + shownPart + hider;
  }
}
