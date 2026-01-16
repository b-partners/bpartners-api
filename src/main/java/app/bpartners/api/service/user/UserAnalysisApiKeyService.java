package app.bpartners.api.service.user;

import static app.bpartners.api.service.user.UserAnalysisApiKeyService.ConsumerType.INSURANCE;

import app.bpartners.api.model.User;
import app.bpartners.api.model.UserAnalysisApiKey;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class UserAnalysisApiKeyService {
  private static final ConsumerType DEFAULT_CONSUMER_TYPE = INSURANCE;
  private static final Double DEFAULT_MAX_SURFACE = 0.0;
  private static final List<DetectableObjectModel> DEFAULT_ALLOWED_MODELS =
      List.of(DetectableObjectModel.ofName("BP_TOITURE"));

  private static final List<AuthorizedZone> DEFAULT_AUTHORIZED_ZONES = List.of(); // deprecated
  private static final List<DetectableObjectType> DEFAULT_DETECTABLE_OBJECT_TYPES =
      List.of(); // deprecated

  private final String geo_jobs_base_url;
  private final String geo_jobs_admin_api_key;
  private final RestTemplate restTemplate;

  public UserAnalysisApiKeyService(
      @Value("geo.jobs.base.url") String geo_jobs_base_url,
      @Value("geo.jobs.admin.api.key") String geo_jobs_admin_api_key,
      RestTemplate restTemplate) {
    this.geo_jobs_base_url = geo_jobs_base_url;
    this.geo_jobs_admin_api_key = geo_jobs_admin_api_key;
    this.restTemplate = restTemplate;
  }

  public UserAnalysisApiKey getAnalysisApiKey(User user) {
    UriComponentsBuilder uriBuilder =
        UriComponentsBuilder.fromHttpUrl(geo_jobs_base_url + "/api/keys");

    HttpHeaders headers = new HttpHeaders();
    headers.add("x-api-key", geo_jobs_admin_api_key);

    HttpEntity<List<AnalysisApiKeyCreation>> request =
        new HttpEntity<>(List.of(toAnalysisApiKeyCreation(user)), headers);

    CreatedAnalysisApiKey[] createdAnalysisApiKeys =
        restTemplate.postForObject(
            uriBuilder.toUriString(), request, CreatedAnalysisApiKey[].class);

    CreatedAnalysisApiKey createdAnalysisApiKey = createdAnalysisApiKeys[0];

    return new UserAnalysisApiKey()
        .toBuilder()
            .user(user)
            .apiKey(createdAnalysisApiKey.getKey())
            .creationDatetime(createdAnalysisApiKey.getCreationDatetime())
            .expirationDatetime(null)
            .enabled(true)
            .build();
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

  @Getter
  @Setter
  @AllArgsConstructor
  @NoArgsConstructor
  static class CreatedAnalysisApiKey {
    private String key;
    private Instant creationDatetime;
  }

  record AnalysisApiKeyCreation(
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
