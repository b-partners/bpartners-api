package app.bpartners.api.service.user;

import static java.time.Instant.now;

import app.bpartners.api.model.User;
import app.bpartners.api.model.UserAnalysisApiKey;
import app.bpartners.api.repository.implementation.UserAnalysisApiKeyRepositoryImpl;
import app.bpartners.api.service.user.analysis.*;
import java.util.List;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class UserAnalysisApiKeyService {
  private final UserAnalysisApiKeyRepositoryImpl userAnalysisApiKeyRepositoryImpl;
  private final AnalysisApiKeyApi analysisApiKeyApi;

  public UserAnalysisApiKeyService(
      UserAnalysisApiKeyRepositoryImpl userAnalysisApiKeyRepositoryImpl,
      AnalysisApiKeyApi analysisApiKeyApi) {
    this.userAnalysisApiKeyRepositoryImpl = userAnalysisApiKeyRepositoryImpl;
    this.analysisApiKeyApi = analysisApiKeyApi;
  }

  @SneakyThrows
  public UserAnalysisApiKey revokeAnalysisApiKey(UserAnalysisApiKey apiKeyToRevoke) {
    String apiKeyToRevokeValue = apiKeyToRevoke.getApiKey();
    ResponseEntity<RevokedAnalysisApiKey> response =
        analysisApiKeyApi.requestAnalysisApiKeyRevocation(apiKeyToRevokeValue);

    if (!response.getStatusCode().is2xxSuccessful()) {
      throw new RuntimeException(
          "API exception occurred while attempting to revoke analysis api key "
              + hide(apiKeyToRevokeValue));
    }

    apiKeyToRevoke.setEnabled(false);
    apiKeyToRevoke.setExpirationDatetime(now());

    return userAnalysisApiKeyRepositoryImpl.save(apiKeyToRevoke);
  }

  @SneakyThrows
  public UserAnalysisApiKey getAnalysisApiKey(User user) {
    ResponseEntity<List<CreatedAnalysisApiKey>> response =
        analysisApiKeyApi.requestAnalysisApiKeyCreation(user);

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

  static String hide(String apiKey) {
    int keyLength = apiKey.length();
    int hideRange = keyLength / (keyLength / 6);
    String shownPart = apiKey.substring(hideRange, (keyLength - hideRange));
    String hider = "*".repeat(hideRange);

    return hider + shownPart + hider;
  }
}
