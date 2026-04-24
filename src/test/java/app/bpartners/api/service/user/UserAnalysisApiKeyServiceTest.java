package app.bpartners.api.service.user;

import static app.bpartners.api.service.user.UserAnalysisApiKeyService.hide;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;

import app.bpartners.api.model.User;
import app.bpartners.api.model.UserAnalysisApiKey;
import app.bpartners.api.repository.implementation.UserAnalysisApiKeyRepositoryImpl;
import app.bpartners.api.service.user.analysis.AnalysisApiKeyApi;
import app.bpartners.api.service.user.analysis.CreatedAnalysisApiKey;
import app.bpartners.api.service.user.analysis.RevokedAnalysisApiKey;
import java.net.URISyntaxException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

class UserAnalysisApiKeyServiceTest {
  UserAnalysisApiKeyRepositoryImpl userAnalysisApiKeyRepositoryMock = mock();
  AnalysisApiKeyApi analysisApiKeyApi = mock();
  UserAnalysisApiKeyService subject =
      new UserAnalysisApiKeyService(userAnalysisApiKeyRepositoryMock, analysisApiKeyApi);

  @Test
  void successfully_revoke_analysis_api_key() throws URISyntaxException {
    String apiKeyStr = "apikey-to-revoke";
    UserAnalysisApiKey apiKey =
        UserAnalysisApiKey.builder().apiKey(apiKeyStr).enabled(true).build();
    RevokedAnalysisApiKey revokedAnalysisApiKey = new RevokedAnalysisApiKey(apiKeyStr, now());
    when(analysisApiKeyApi.requestAnalysisApiKeyRevocation(apiKeyStr))
        .thenReturn(ResponseEntity.ok(revokedAnalysisApiKey));
    when(userAnalysisApiKeyRepositoryMock.save(any())).thenAnswer(i -> i.getArgument(0));

    UserAnalysisApiKey actual = subject.revokeAnalysisApiKey(apiKey);

    assertFalse(actual.isEnabled());
    assertNotNull(actual.getExpirationDatetime());
    assertEquals(apiKeyStr, actual.getApiKey());
    verify(userAnalysisApiKeyRepositoryMock).save(apiKey);
  }

  @Test
  void throw_exception_when_revocation_api_fails() throws URISyntaxException {
    String apiKeyStr = "apikey-to-revoke";
    UserAnalysisApiKey apiKey = UserAnalysisApiKey.builder().apiKey(apiKeyStr).build();
    when(analysisApiKeyApi.requestAnalysisApiKeyRevocation(apiKeyStr))
        .thenReturn(ResponseEntity.status(BAD_REQUEST).build());

    var actualException =
        assertThrows(RuntimeException.class, () -> subject.revokeAnalysisApiKey(apiKey));

    assertEquals(
        "API exception occurred while attempting to revoke analysis api key " + hide(apiKeyStr),
        actualException.getMessage());
    verify(userAnalysisApiKeyRepositoryMock, never()).save(any());
  }

  @Test
  void successfully_get_analysis_api_key() throws URISyntaxException {
    var createdApiKey = new CreatedAnalysisApiKey("apikey", now());
    var responseEntityMock = mock(ResponseEntity.class);
    when(responseEntityMock.getStatusCode()).thenReturn(OK);
    when(analysisApiKeyApi.requestAnalysisApiKeyCreation(any())).thenReturn(responseEntityMock);
    when(responseEntityMock.getBody()).thenReturn(List.of(createdApiKey));

    UserAnalysisApiKey actual = subject.getAnalysisApiKey(mock());

    assertEquals(createdApiKey.getKey(), actual.getApiKey());
  }

  @Test
  void throw_exception_when_api_exception_occurs() throws URISyntaxException {
    var responseEntityMock = mock(ResponseEntity.class);
    var userMock = mock(User.class);
    var userEmail = randomUUID() + "@email.com";

    when(userMock.getEmail()).thenReturn(userEmail);
    when(responseEntityMock.getStatusCode()).thenReturn(BAD_REQUEST);
    when(analysisApiKeyApi.requestAnalysisApiKeyCreation(any())).thenReturn(responseEntityMock);

    var actualException =
        assertThrows(RuntimeException.class, () -> subject.getAnalysisApiKey(userMock));

    assertEquals(
        "API exception occurred while attempting to create user.email="
            + userEmail
            + " analysis api key",
        actualException.getMessage());
  }

  @Test
  void throw_exception_when_api_returns_empty_list() throws URISyntaxException {
    var responseEntityMock = mock(ResponseEntity.class);
    var userMock = mock(User.class);
    var userEmail = randomUUID() + "@email.com";

    when(userMock.getEmail()).thenReturn(userEmail);
    when(responseEntityMock.getStatusCode()).thenReturn(OK);
    when(responseEntityMock.getBody()).thenReturn(List.of());
    when(analysisApiKeyApi.requestAnalysisApiKeyCreation(any())).thenReturn(responseEntityMock);

    var actualException =
        assertThrows(RuntimeException.class, () -> subject.getAnalysisApiKey(userMock));

    assertEquals(
        "API failed to create user.email=" + userEmail + " analysis api key",
        actualException.getMessage());
  }
}
