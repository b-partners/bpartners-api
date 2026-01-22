package app.bpartners.api.service.user;

import static app.bpartners.api.endpoint.rest.model.UserApiKeyType.ANALYSIS;
import static app.bpartners.api.endpoint.rest.model.UserApiKeyType.DASHBOARD;

import app.bpartners.api.endpoint.rest.model.RevokeApiKey;
import app.bpartners.api.endpoint.rest.model.RevokedApiKey;
import app.bpartners.api.endpoint.rest.model.UserApiKeyType;
import app.bpartners.api.model.User;
import app.bpartners.api.model.UserAnalysisApiKey;
import app.bpartners.api.repository.UserAnalysisApiKeyRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ApiKeyService {
  private final UserService userService;
  private final UserAnalysisApiKeyRepository userAnalysisApiKeyRepository;

  public List<RevokedApiKey> revokeApiKeys(List<RevokeApiKey> revokeApiKeys) {
    return revokeApiKeys.stream().map(this::revokeApiKey).toList();
  }

  private RevokedApiKey revokeApiKey(RevokeApiKey revokeApiKey) {
    String key = revokeApiKey.getKey();

    User user;
    UserApiKeyType apiKeyType = DASHBOARD;
    UserAnalysisApiKey analysisApiKey = userAnalysisApiKeyRepository.getByApiKey(key);

    if (analysisApiKey != null) {
      UserAnalysisApiKey revokedAnalysisApiKey = analysisApiKey.toBuilder().enabled(false).build();
      userAnalysisApiKeyRepository.save(revokedAnalysisApiKey);

      user = analysisApiKey.getUser();
      apiKeyType = ANALYSIS;
    } else {
      user = userService.getUserByApiKey(key);

      if (user != null) {
        userService.save(user.toBuilder().apiKey(null).build());
      }
    }

    if (user == null) {
      return null;
    }

    return new RevokedApiKey().apiKey(key).userId(user.getId()).type(apiKeyType);
  }
}
