package app.bpartners.api.service.user;

import app.bpartners.api.model.User;
import app.bpartners.api.model.UserAnalysisApiKey;
import app.bpartners.api.repository.implementation.UserAnalysisApiKeyRepositoryImpl;
import app.bpartners.api.service.user.analysis.*;
import java.util.List;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
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
    RevokedAnalysisApiKey revokedAnalysisApiKey =
        analysisApiKeyApi.requestAnalysisApiKeyRevocation(apiKeyToRevokeValue);

    apiKeyToRevoke.setEnabled(false);
    apiKeyToRevoke.setExpirationDatetime(revokedAnalysisApiKey.revokedAt());

    return userAnalysisApiKeyRepositoryImpl.save(apiKeyToRevoke);
  }

  @SneakyThrows
  public UserAnalysisApiKey getAnalysisApiKey(User user) {
    List<CreatedAnalysisApiKey> createdAnalysisApiKeys =
        analysisApiKeyApi.createAnalysisApiKeys(user);

    var createdAnalysisApiKey = createdAnalysisApiKeys.getFirst();

    return new UserAnalysisApiKey()
        .toBuilder()
            .user(user)
            .apiKey(createdAnalysisApiKey.getKey())
            .creationDatetime(createdAnalysisApiKey.getCreationDatetime())
            .expirationDatetime(null)
            .enabled(true)
            .build();
  }
}
