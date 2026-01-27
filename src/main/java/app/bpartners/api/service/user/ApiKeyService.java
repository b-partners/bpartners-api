package app.bpartners.api.service.user;

import static app.bpartners.api.endpoint.rest.model.UserApiKeyType.ANALYSIS;
import static app.bpartners.api.endpoint.rest.model.UserApiKeyType.DASHBOARD;

import app.bpartners.api.endpoint.rest.model.UserApiKey;
import app.bpartners.api.model.UserAnalysisApiKey;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.repository.UserAnalysisApiKeyRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ApiKeyService {
  private final UserService userService;
  private final UserAnalysisApiKeyRepository userAnalysisApiKeyRepository;

  public List<UserApiKey> revokeApiKeys(List<String> keys) {
    if (keys.stream().anyMatch(String::isEmpty)) {
      throw new BadRequestException("Api keys can not be null or empty");
    }
    return keys.stream().map(this::revokeApiKey).toList();
  }

  private UserApiKey revokeApiKey(String key) {
    UserAnalysisApiKey userAnalysisApiKey = userAnalysisApiKeyRepository.getByApiKey(key);
    if (userAnalysisApiKey != null) {
      UserAnalysisApiKey revokedAnalysisApiKey =
          userAnalysisApiKeyRepository.save(userAnalysisApiKey.toBuilder().enabled(false).build());
      return new UserApiKey()
          .key(revokedAnalysisApiKey.getApiKey())
          .enabled(revokedAnalysisApiKey.isEnabled())
          .type(ANALYSIS)
          .expirationDatetime(revokedAnalysisApiKey.getExpirationDatetime())
          .creationDatetime(revokedAnalysisApiKey.getCreationDatetime());
    }
    var user = userService.getUserByApiKey(key);
    var savedUserWithRevokedApiKey = userService.save(user.toBuilder().apiKey(null).build());
    return new UserApiKey()
        .key(user.getApiKey())
        .enabled(savedUserWithRevokedApiKey.getApiKey() != null)
        .type(DASHBOARD)
        .expirationDatetime(null)
        .creationDatetime(null); // TODO
  }
}
