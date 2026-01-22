package app.bpartners.api.service.user;

import static app.bpartners.api.endpoint.rest.model.UserApiKeyType.ANALYSIS;
import static app.bpartners.api.endpoint.rest.model.UserApiKeyType.DASHBOARD;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.rest.model.RevokeApiKey;
import app.bpartners.api.endpoint.rest.model.RevokedApiKey;
import app.bpartners.api.model.User;
import app.bpartners.api.model.UserAnalysisApiKey;
import app.bpartners.api.repository.UserAnalysisApiKeyRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class ApiKeyServiceTest {
  private static final String DASHBOARD_KEY = randomUUID().toString();
  private static final String ANALYSIS_KEY = randomUUID().toString();
  private static final Instant CREATION_DATETIME = now();

  private static final String USER_1_UUID = randomUUID().toString();
  private static final String USER_2_UUID = randomUUID().toString();

  UserService userServiceMock = mock(UserService.class);
  UserAnalysisApiKeyRepository userAnalysisApiKeyRepositoryMock =
      mock(UserAnalysisApiKeyRepository.class);

  ApiKeyService subject = new ApiKeyService(userServiceMock, userAnalysisApiKeyRepositoryMock);

  @Test
  void revoke_mixed_api_keys() {
    when(userAnalysisApiKeyRepositoryMock.getByApiKey(DASHBOARD_KEY)).thenReturn(null);
    when(userAnalysisApiKeyRepositoryMock.getByApiKey(ANALYSIS_KEY))
        .thenReturn(analysisApiKeyToRevoke());
    doNothing().when(userAnalysisApiKeyRepositoryMock).save(any());
    when(userServiceMock.getUserByApiKey(ANALYSIS_KEY)).thenReturn(null);
    when(userServiceMock.getUserByApiKey(DASHBOARD_KEY)).thenReturn(user2());

    var mixedRevokeApiKeys = List.of(dashboardRevokeApiKey(), analysisRevokeApiKey());
    var expected = List.of(revokedDashboardApiKey(), revokedAnalysisApiKey());

    var actual = subject.revokeApiKeys(mixedRevokeApiKeys);

    assertEquals(expected, actual);
  }

  private RevokedApiKey revokedDashboardApiKey() {
    return new RevokedApiKey().type(DASHBOARD).userId(user2().getId()).apiKey(DASHBOARD_KEY);
  }

  private RevokedApiKey revokedAnalysisApiKey() {
    return new RevokedApiKey().type(ANALYSIS).userId(user1().getId()).apiKey(ANALYSIS_KEY);
  }

  private RevokeApiKey dashboardRevokeApiKey() {
    return new RevokeApiKey().key(DASHBOARD_KEY);
  }

  private RevokeApiKey analysisRevokeApiKey() {
    return new RevokeApiKey().key(ANALYSIS_KEY);
  }

  private UserAnalysisApiKey analysisApiKeyToRevoke() {
    return UserAnalysisApiKey.builder()
        .apiKey(ANALYSIS_KEY)
        .user(user1())
        .creationDatetime(CREATION_DATETIME)
        .expirationDatetime(null)
        .enabled(true)
        .build();
  }

  private User user1() {
    return User.builder().id(USER_1_UUID).build();
  }

  private User user2() {
    return User.builder().id(USER_2_UUID).apiKey(DASHBOARD_KEY).build();
  }
}
