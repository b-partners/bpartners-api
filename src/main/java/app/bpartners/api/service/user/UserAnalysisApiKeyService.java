package app.bpartners.api.service.user;

import static app.bpartners.api.service.user.analysis.ConsumerType.INSURANCE;
import static java.time.Instant.now;
import static org.springframework.http.HttpMethod.POST;

import app.bpartners.api.model.User;
import app.bpartners.api.model.UserAnalysisApiKey;
import app.bpartners.api.repository.implementation.UserAnalysisApiKeyRepositoryImpl;
import app.bpartners.api.service.user.analysis.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Service
public class UserAnalysisApiKeyService {
  private static final ConsumerType DEFAULT_CONSUMER_TYPE = INSURANCE;
  private static final Double DEFAULT_MAX_SURFACE = 0.0;
  private static final List<DetectableObjectModel> DEFAULT_ALLOWED_MODELS =
      List.of(DetectableObjectModel.ofName("BP_TOITURE"));

  private static final List<AuthorizedZone> DEFAULT_AUTHORIZED_ZONES = List.of(); // deprecated
  private static final List<DetectableObjectType> DEFAULT_DETECTABLE_OBJECT_TYPES =
      List.of(); // deprecated
  public static final String AUTHORIZATION_HEADER = "x-api-key";

  private final String geoJobsBaseUrl;
  private final String geoJobsAdminApiKey;
  private final RestTemplate restTemplate;
  private final UserAnalysisApiKeyRepositoryImpl userAnalysisApiKeyRepositoryImpl;

  public UserAnalysisApiKeyService(
      @Value("${geo.jobs.base.url}") String geoJobsBaseUrl,
      @Value("${geo.jobs.admin.api.key}") String geoJobsAdminApiKey,
      RestTemplate restTemplate,
      UserAnalysisApiKeyRepositoryImpl userAnalysisApiKeyRepositoryImpl) {
    this.geoJobsBaseUrl = geoJobsBaseUrl;
    this.geoJobsAdminApiKey = geoJobsAdminApiKey;
    this.restTemplate = restTemplate;
    this.userAnalysisApiKeyRepositoryImpl = userAnalysisApiKeyRepositoryImpl;
  }

  @SneakyThrows
  public UserAnalysisApiKey revokeAnalysisApiKey(UserAnalysisApiKey apiKeyToRevoke) {
    ResponseEntity<List<CreatedAnalysisApiKey>> response =
        requestAnalysisApiKeyRevocation(apiKeyToRevoke.getApiKey());

    if (!response.getStatusCode().is2xxSuccessful()) {
      User targetUser = apiKeyToRevoke.getUser();
      throw new RuntimeException(
          "API exception occurred while attempting to revoke analysis api key "
              + apiKeyToRevoke.getApiKey()
              + " for user.email="
              + targetUser.getEmail());
    }

    apiKeyToRevoke.setEnabled(false);
    apiKeyToRevoke.setExpirationDatetime(now());

    return userAnalysisApiKeyRepositoryImpl.save(apiKeyToRevoke);
  }

  @SneakyThrows
  public UserAnalysisApiKey getAnalysisApiKey(User user) {
    var uriBuilder = UriComponentsBuilder.fromUri(new URI(geoJobsBaseUrl + "/api/keys"));
    var uriString = uriBuilder.toUriString();

    var headers = new HttpHeaders();
    headers.add(AUTHORIZATION_HEADER, geoJobsAdminApiKey);

    var request = new HttpEntity<>(List.of(toAnalysisApiKeyCreation(user)), headers);

    ResponseEntity<List<CreatedAnalysisApiKey>> response =
        restTemplate.exchange(uriString, POST, request, new ParameterizedTypeReference<>() {});

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
    var createdAnalysisApiKey = response.getBody().getFirst();

    return new UserAnalysisApiKey()
        .toBuilder()
            .user(user)
            .apiKey(createdAnalysisApiKey.getKey())
            .creationDatetime(createdAnalysisApiKey.getCreationDatetime())
            .expirationDatetime(null)
            .enabled(true)
            .build();
  }

  private @NotNull ResponseEntity<List<CreatedAnalysisApiKey>> requestAnalysisApiKeyRevocation(
      String apiKeyValue) throws URISyntaxException {
    var uriBuilder = UriComponentsBuilder.fromUri(new URI(geoJobsBaseUrl + "/api/keys"));
    var uriString = uriBuilder.toUriString();

    var headers = new HttpHeaders();
    headers.add(AUTHORIZATION_HEADER, apiKeyValue);

    var request = new HttpEntity<>(new AnalysisApiKeyRevocation(apiKeyValue), headers);

    return restTemplate.exchange(uriString, POST, request, new ParameterizedTypeReference<>() {});
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
