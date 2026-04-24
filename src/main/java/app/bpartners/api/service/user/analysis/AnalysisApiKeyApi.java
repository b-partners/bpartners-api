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
import org.springframework.http.ResponseEntity;
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
  public static final String AUTHORIZATION_HEADER = "x-api-key";
  public static final String API_KEY_API_PATH = "/api/keys";

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

  public @NotNull ResponseEntity<List<CreatedAnalysisApiKey>> requestAnalysisApiKeyCreation(User user)
      throws URISyntaxException {
    var uriString = getAnalysisApiKeyApiUri();

    var headers = new HttpHeaders();
    headers.add(AUTHORIZATION_HEADER, geoJobsAdminApiKey);

    var request = new HttpEntity<>(List.of(toAnalysisApiKeyCreation(user)), headers);

    return restTemplate.exchange(uriString, POST, request, new ParameterizedTypeReference<>() {});
  }

  public @NotNull ResponseEntity<RevokedAnalysisApiKey> requestAnalysisApiKeyRevocation(
      String apiKeyToRevoke) throws URISyntaxException {
    var uriString = getAnalysisApiKeyApiUri();

    var headers = new HttpHeaders();
    headers.add(AUTHORIZATION_HEADER, geoJobsAdminApiKey);

    var request = new HttpEntity<>(new AnalysisApiKeyRevocation(apiKeyToRevoke), headers);

    return restTemplate.exchange(uriString, DELETE, request, new ParameterizedTypeReference<>() {});
  }

  private @NotNull String getAnalysisApiKeyApiUri() throws URISyntaxException {
    UriComponentsBuilder uriBuilder =
        UriComponentsBuilder.fromUri(new URI(geoJobsBaseUrl + API_KEY_API_PATH));
    return uriBuilder.toUriString();
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
}
