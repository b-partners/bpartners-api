package app.bpartners.api.service.user;

import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.api.model.User;
import app.bpartners.api.model.UserAnalysisApiKey;
import app.bpartners.api.repository.implementation.UserAnalysisApiKeyRepositoryImpl;
import app.bpartners.api.service.user.analysis.AnalysisApiKeyApi;
import app.bpartners.api.service.user.analysis.CreatedAnalysisApiKey;
import app.bpartners.api.service.user.analysis.RevokedAnalysisApiKey;
import java.util.List;
import org.junit.jupiter.api.Test;

class UserAnalysisApiKeyServiceTest {
  UserAnalysisApiKeyRepositoryImpl userAnalysisApiKeyRepositoryMock = mock();
  AnalysisApiKeyApi analysisApiKeyApi = mock();
  UserAnalysisApiKeyService subject =
      new UserAnalysisApiKeyService(userAnalysisApiKeyRepositoryMock, analysisApiKeyApi);

  @Test
  void successfully_revoke_analysis_api_key() {
    String apiKeyStr = "apikey-to-revoke";
    UserAnalysisApiKey apiKey =
        UserAnalysisApiKey.builder().apiKey(apiKeyStr).enabled(true).build();
    RevokedAnalysisApiKey revokedAnalysisApiKey = new RevokedAnalysisApiKey(apiKeyStr, now());
    when(analysisApiKeyApi.requestAnalysisApiKeyRevocation(apiKeyStr))
        .thenReturn(revokedAnalysisApiKey);
    when(userAnalysisApiKeyRepositoryMock.save(any())).thenAnswer(i -> i.getArgument(0));

    UserAnalysisApiKey actual = subject.revokeAnalysisApiKey(apiKey);

    assertFalse(actual.isEnabled());
    assertNotNull(actual.getExpirationDatetime());
    assertEquals(apiKeyStr, actual.getApiKey());
    verify(userAnalysisApiKeyRepositoryMock).save(apiKey);
  }

  @Test
  void throw_exception_when_revocation_api_fails() {
    String apiKeyStr = "apikey-to-revoke";
    UserAnalysisApiKey apiKey = UserAnalysisApiKey.builder().apiKey(apiKeyStr).build();
    when(analysisApiKeyApi.requestAnalysisApiKeyRevocation(apiKeyStr))
        .thenThrow(
            new RuntimeException(
                "API exception occurred while attempting to revoke analysis api key "
                    + hide(apiKeyStr)));

    var actualException =
        assertThrows(RuntimeException.class, () -> subject.revokeAnalysisApiKey(apiKey));

    assertEquals(
        "API exception occurred while attempting to revoke analysis api key " + hide(apiKeyStr),
        actualException.getMessage());
    verify(userAnalysisApiKeyRepositoryMock, never()).save(any());
  }

  @Test
  void successfully_get_analysis_api_key() {
    var createdApiKey = new CreatedAnalysisApiKey("apikey", now());
    when(analysisApiKeyApi.createAnalysisApiKeys(any())).thenReturn(List.of(createdApiKey));

    UserAnalysisApiKey actual = subject.getAnalysisApiKey(mock());

    assertEquals(createdApiKey.getKey(), actual.getApiKey());
  }

  @Test
  void throw_exception_when_api_exception_occurs() {
    var userMock = mock(User.class);
    var userEmail = randomUUID() + "@email.com";

    when(userMock.getEmail()).thenReturn(userEmail);
    when(analysisApiKeyApi.createAnalysisApiKeys(any()))
        .thenThrow(
            new RuntimeException(
                "API exception occurred while attempting to create user.email="
                    + userEmail
                    + " analysis api key"));

    var actualException =
        assertThrows(RuntimeException.class, () -> subject.getAnalysisApiKey(userMock));

    assertEquals(
        "API exception occurred while attempting to create user.email="
            + userEmail
            + " analysis api key",
        actualException.getMessage());
  }

  @Test
  void throw_exception_when_api_returns_empty_list() {
    var userMock = mock(User.class);
    var userEmail = randomUUID() + "@email.com";

    when(userMock.getEmail()).thenReturn(userEmail);
    when(analysisApiKeyApi.createAnalysisApiKeys(any())).thenReturn(List.of());

    assertThrows(RuntimeException.class, () -> subject.getAnalysisApiKey(userMock));
  }

  static String hide(String apiKey) {
    int keyLength = apiKey.length();
    int hideRange = keyLength / (keyLength / 6);
    String shownPart = apiKey.substring(hideRange, (keyLength - hideRange));
    String hider = "*".repeat(hideRange);

    return hider + shownPart + hider;
  }
}
