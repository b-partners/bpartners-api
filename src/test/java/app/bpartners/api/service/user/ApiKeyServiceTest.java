package app.bpartners.api.service.user;

import static app.bpartners.api.endpoint.rest.model.UserApiKeyType.ANALYSIS;
import static app.bpartners.api.endpoint.rest.model.UserApiKeyType.DASHBOARD;
import static app.bpartners.api.endpoint.rest.security.model.Role.ADMIN_ROLE;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.rest.model.RevokeApiKey;
import app.bpartners.api.endpoint.rest.model.UserApiKey;
import app.bpartners.api.model.User;
import app.bpartners.api.model.UserAnalysisApiKey;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.ForbiddenException;
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
  UserAnalysisApiKeyService userAnalysisApiKeyServiceMock = mock(UserAnalysisApiKeyService.class);

  ApiKeyService subject =
      new ApiKeyService(
          userServiceMock, userAnalysisApiKeyRepositoryMock, userAnalysisApiKeyServiceMock);

  @Test
  void non_admin_cannot_revoke_others_analysis_api_key() {
    when(userAnalysisApiKeyRepositoryMock.getByApiKey(ANALYSIS_KEY))
        .thenReturn(analysisApiKeyToRevoke());
    when(userAnalysisApiKeyRepositoryMock.save(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(userServiceMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(userServiceMock.getUserByApiKey(ANALYSIS_KEY)).thenReturn(null);
    when(userAnalysisApiKeyServiceMock.revokeAnalysisApiKey(analysisApiKeyToRevoke()))
        .thenReturn(analysisApiKeyToRevoke().toBuilder().enabled(false).build());
    var key = analysisRevokeApiKey().getKey();
    var keys = List.of(key);
    var user = user2();

    var actualException =
        assertThrows(ForbiddenException.class, () -> subject.revokeApiKeys(keys, user));

    assertEquals("Users can only revoke it's own api key", actualException.getMessage());
  }

  @Test
  void non_admin_cannot_revoke_others_dashboard_api_key() {
    when(userAnalysisApiKeyRepositoryMock.getByApiKey(DASHBOARD_KEY)).thenReturn(null);
    when(userServiceMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(userServiceMock.getUserByApiKey(DASHBOARD_KEY)).thenReturn(user2());
    var key = dashboardRevokeApiKey().getKey();
    var keys = List.of(key);
    var user = user1();

    var actualException =
        assertThrows(ForbiddenException.class, () -> subject.revokeApiKeys(keys, user));

    assertEquals("Users can only revoke it's own api key", actualException.getMessage());
  }

  @Test
  void throw_bad_request_on_empty_keys() {
    var actualException =
        assertThrows(
            BadRequestException.class, () -> subject.revokeApiKeys(List.of(""), adminUser()));

    assertEquals("Api keys can not be null or empty", actualException.getMessage());
  }

  @Test
  void admin_revoke_mixed_api_keys() {
    when(userAnalysisApiKeyRepositoryMock.getByApiKey(DASHBOARD_KEY)).thenReturn(null);
    when(userAnalysisApiKeyRepositoryMock.getByApiKey(ANALYSIS_KEY))
        .thenReturn(analysisApiKeyToRevoke());
    when(userAnalysisApiKeyRepositoryMock.save(any()))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(userServiceMock.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    when(userServiceMock.getUserByApiKey(ANALYSIS_KEY)).thenReturn(null);
    when(userServiceMock.getUserByApiKey(DASHBOARD_KEY)).thenReturn(user2());
    when(userAnalysisApiKeyServiceMock.revokeAnalysisApiKey(analysisApiKeyToRevoke()))
        .thenReturn(analysisApiKeyToRevoke().toBuilder().enabled(false).build());

    var expected = List.of(revokedDashboardApiKey(), revokedAnalysisApiKey());

    var actual =
        subject.revokeApiKeys(
            List.of(dashboardRevokeApiKey().getKey(), analysisRevokeApiKey().getKey()),
            adminUser());

    assertEquals(expected, actual);
  }

  private User adminUser() {
    return User.builder().roles(List.of(ADMIN_ROLE)).build();
  }

  private UserApiKey revokedDashboardApiKey() {
    return new UserApiKey().type(DASHBOARD).enabled(false).key(DASHBOARD_KEY);
  }

  private UserApiKey revokedAnalysisApiKey() {
    return new UserApiKey()
        .type(ANALYSIS)
        .enabled(false)
        .key(ANALYSIS_KEY)
        .creationDatetime(CREATION_DATETIME);
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
    return User.builder().id(USER_1_UUID).roles(List.of()).build();
  }

  private User user2() {
    return User.builder().id(USER_2_UUID).roles(List.of()).apiKey(DASHBOARD_KEY).build();
  }
}
