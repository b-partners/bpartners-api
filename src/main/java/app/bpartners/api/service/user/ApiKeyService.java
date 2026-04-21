package app.bpartners.api.service.user;

import static app.bpartners.api.endpoint.rest.model.UserApiKeyType.ANALYSIS;
import static app.bpartners.api.endpoint.rest.model.UserApiKeyType.DASHBOARD;
import static app.bpartners.api.endpoint.rest.security.model.Role.ADMIN_ROLE;

import app.bpartners.api.endpoint.rest.model.UserApiKey;
import app.bpartners.api.model.User;
import app.bpartners.api.model.UserAnalysisApiKey;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.ForbiddenException;
import app.bpartners.api.repository.UserAnalysisApiKeyRepository;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class ApiKeyService {
  private final UserService userService;
  private final UserAnalysisApiKeyRepository userAnalysisApiKeyRepository;
  private final UserAnalysisApiKeyService userAnalysisApiKeyService;

  @Transactional
  public List<UserApiKey> revokeApiKeys(List<String> keys, User user) {
    if (keys.stream().anyMatch(String::isEmpty)) {
      throw new BadRequestException("Api keys can not be null or empty");
    }
    return keys.stream().map(key -> revokeApiKey(key, user)).toList();
  }

  private UserApiKey revokeApiKey(String key, User authenticatedUser) {
    UserAnalysisApiKey userAnalysisApiKey = userAnalysisApiKeyRepository.getByApiKey(key);
    if (userAnalysisApiKey != null) {
      return revokeAnalysisApiKey(authenticatedUser, userAnalysisApiKey);
    }
    return revokeUserApiKey(key, authenticatedUser);
  }

  private UserApiKey revokeAnalysisApiKey(
      User authenticatedUser, UserAnalysisApiKey userAnalysisApiKey) {
    User apiKeyOwner = userAnalysisApiKey.getUser();
    if (!Objects.equals(apiKeyOwner.getId(), authenticatedUser.getId())
        && !authenticatedUser.getRoles().contains(ADMIN_ROLE)) {
      throw new ForbiddenException("Users can only revoke it's own api key");
    }

    UserAnalysisApiKey revokedAnalysisApiKey =
        userAnalysisApiKeyService.revokeAnalysisApiKey(userAnalysisApiKey);
    return new UserApiKey()
        .key(revokedAnalysisApiKey.getApiKey())
        .enabled(revokedAnalysisApiKey.isEnabled())
        .type(ANALYSIS)
        .expirationDatetime(revokedAnalysisApiKey.getExpirationDatetime())
        .creationDatetime(revokedAnalysisApiKey.getCreationDatetime());
  }

  private UserApiKey revokeUserApiKey(String key, User authenticatedUser) {
    var userApiKeyOwner = userService.getUserByApiKey(key);
    if (userApiKeyOwner == null) {
      throw new BadRequestException("No users found with api key " + key);
    }
    if (!Objects.equals(userApiKeyOwner.getId(), authenticatedUser.getId())
        && !authenticatedUser.getRoles().contains(ADMIN_ROLE)) {
      throw new ForbiddenException("Users can only revoke it's own api key");
    }

    var savedUserWithRevokedApiKey =
        userService.save(userApiKeyOwner.toBuilder().apiKey(null).build());
    return new UserApiKey()
        .key(userApiKeyOwner.getApiKey())
        .enabled(savedUserWithRevokedApiKey.getApiKey() != null)
        .type(DASHBOARD)
        .expirationDatetime(null)
        .creationDatetime(null);
  }
}
