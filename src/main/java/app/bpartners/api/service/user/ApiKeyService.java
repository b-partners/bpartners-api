package app.bpartners.api.service.user;

import static app.bpartners.api.endpoint.rest.model.UserApiKeyType.ANALYSIS;
import static app.bpartners.api.endpoint.rest.model.UserApiKeyType.DASHBOARD;

import app.bpartners.api.endpoint.rest.model.RevokeApiKey;
import app.bpartners.api.endpoint.rest.model.UserApiKey;
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

  public List<UserApiKey> revokeApiKeys(List<RevokeApiKey> revokeApiKeys) {
    return revokeApiKeys.stream().map(this::revokeApiKey).toList();
  }

  private UserApiKey revokeApiKey(RevokeApiKey revokeApiKey) {
    String key = revokeApiKey.getKey();

    User user;
    UserAnalysisApiKey analysisApiKey = userAnalysisApiKeyRepository.getByApiKey(key);
    UserApiKey revokedApiKey = new UserApiKey();

    if (analysisApiKey != null) {
      UserAnalysisApiKey revokedAnalysisApiKey = analysisApiKey.toBuilder().enabled(false).build();
      userAnalysisApiKeyRepository.save(revokedAnalysisApiKey);

      user = analysisApiKey.getUser();
      revokedApiKey.setType(ANALYSIS);
      revokedApiKey.setCreationDatetime(revokedAnalysisApiKey.getCreationDatetime());
      revokedApiKey.setExpirationDatetime(revokedAnalysisApiKey.getExpirationDatetime());
    } else {
      user = userService.getUserByApiKey(key);

      if (user != null) {
        userService.save(user.toBuilder().apiKey(null).build());

        revokedApiKey.setType(DASHBOARD);
      }
    }

    if (user == null) {
      return null;
    }

    revokedApiKey.setKey(key);
    revokedApiKey.setEnabled(false);

    return revokedApiKey;
  }
}
