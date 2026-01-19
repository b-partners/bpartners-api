package app.bpartners.api.service.user;

import static app.bpartners.api.service.user.UserAnalysisApiKeyService.ConsumerType.INSURANCE;
import static org.springframework.http.HttpMethod.POST;

import app.bpartners.api.model.User;
import app.bpartners.api.model.UserAnalysisApiKey;
import com.fasterxml.jackson.annotation.JsonIgnore;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
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

  private final String geoJobsBaseUrl;
  private final String geoJobsAdminApiKey;
  private final RestTemplate restTemplate;

  public UserAnalysisApiKeyService(
      @Value("${geo.jobs.base.url}") String geoJobsBaseUrl,
      @Value("${geo.jobs.admin.api.key}") String geoJobsAdminApiKey,
      RestTemplate restTemplate) {
    this.geoJobsBaseUrl = geoJobsBaseUrl;
    this.geoJobsAdminApiKey = geoJobsAdminApiKey;
    this.restTemplate = restTemplate;
  }

  @SneakyThrows
  public UserAnalysisApiKey getAnalysisApiKey(User user) {
    var uriBuilder = UriComponentsBuilder.fromUri(new URI(geoJobsBaseUrl + "/api/keys"));
    var uriString = uriBuilder.toUriString();

    var headers = new HttpHeaders();
    headers.add("x-api-key", geoJobsAdminApiKey);

    var request = new HttpEntity<>(List.of(toAnalysisApiKeyCreation(user)), headers);

    log.info("URI {}", uriString);
    log.info("Request headers {}", request.getHeaders());
    log.info("Request body {}", request.getBody());

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
      @JsonIgnore List<AuthorizedZone> authorizedZones,
      @JsonIgnore List<DetectableObjectType> detectableObjectTypes) {}

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
