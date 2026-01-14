package app.bpartners.api.service.user;

import app.bpartners.api.model.User;
import app.bpartners.api.model.UserAnalysisApiKey;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class UserAnalysisApiKeyService {
  private static final String GEOJOBS_API_KEY = System.getenv("GEOJOBS_ADMIN_API_KEY");
  private static final ConsumerType DEFAULT_CONSUMER_TYPE = ConsumerType.INSURANCE;
  private static final Double DEFAULT_MAX_SURFACE = 0.0;
  private static final List<DetectableObjectModel> DEFAULT_ALLOWED_MODELS =
      List.of(DetectableObjectModel.ofName("BP_TOITURE"));

  private static final List<AuthorizedZone> DEFAULT_AUTHORIZED_ZONES = List.of(); // deprecated
  private static final List<DetectableObjectType> DEFAULT_DETECTABLE_OBJECT_TYPES =
      List.of(); // deprecated

  private final String GEOJOBS_BASE_URL;
  private final RestTemplate restTemplate;

  public UserAnalysisApiKeyService(@Value("geojobs.base.url") String GEOJOBS_BASE_URL, RestTemplate restTemplate) {
    this.GEOJOBS_BASE_URL = GEOJOBS_BASE_URL;
    this.restTemplate = restTemplate;
  }

  public UserAnalysisApiKey getAnalysisApiKey(User user) {
    UriComponentsBuilder uriBuilder =
        UriComponentsBuilder.fromHttpUrl(GEOJOBS_BASE_URL + "/api/keys");

    HttpHeaders headers = new HttpHeaders();
    headers.add("x-api-key", GEOJOBS_API_KEY);

    HttpEntity<ApiKeyCreationPayload> request = new HttpEntity<>(creationPayload(user), headers);

    CreatedAnalysisApiKey createdAnalysisApiKey =
        restTemplate.postForObject(uriBuilder.toUriString(), request, CreatedAnalysisApiKey.class);

    return new UserAnalysisApiKey()
        .toBuilder()
            .userId(user.getId())
            .apiKey(createdAnalysisApiKey.key())
            .creationDatetime(createdAnalysisApiKey.creationDatetime())
            .expirationDatetime(null)
            .build();
  }

  private ApiKeyCreationPayload creationPayload(User user) {
    return new ApiKeyCreationPayload(
        user.getName(),
        user.getEmail(),
        DEFAULT_CONSUMER_TYPE,
        DEFAULT_MAX_SURFACE,
        DEFAULT_ALLOWED_MODELS,
        DEFAULT_AUTHORIZED_ZONES,
        DEFAULT_DETECTABLE_OBJECT_TYPES);
  }

  record CreatedAnalysisApiKey(String key, Instant creationDatetime) {}

  record ApiKeyCreationPayload(
      String consumerName,
      String consumerEmail,
      ConsumerType consumerType,
      Double maxSurface,
      List<DetectableObjectModel> allowedModels,
      List<AuthorizedZone> authorizedZones, // deprecated
      List<DetectableObjectType> detectableObjectTypes // deprecated
      ) {}

  enum ConsumerType {
    INSURANCE,
    COMMUNITY,
    ADMIN
  }

  record DetectableObjectModel(String modelName) {
    static DetectableObjectModel ofName(String modelName) {
      return new DetectableObjectModel(modelName);
    }
  }

  @Deprecated
  record AuthorizedZone() {}

  @Deprecated
  record DetectableObjectType() {}
}
