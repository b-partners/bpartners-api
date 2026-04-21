package app.bpartners.api.service.user;

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
  void getAnalysisApiKey_ok() throws URISyntaxException {
    var createdApiKey = new CreatedAnalysisApiKey("apikey", now());
    var responseEntityMock = mock(ResponseEntity.class);

    when(responseEntityMock.getStatusCode()).thenReturn(OK);
    when(analysisApiKeyApi.requestAnalysisApiKeys(any())).thenReturn(responseEntityMock);
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
    when(analysisApiKeyApi.requestAnalysisApiKeys(any())).thenReturn(responseEntityMock);

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
    when(analysisApiKeyApi.requestAnalysisApiKeys(any())).thenReturn(responseEntityMock);

    var actualException =
        assertThrows(RuntimeException.class, () -> subject.getAnalysisApiKey(userMock));

    assertEquals(
        "API failed to create user.email=" + userEmail + " analysis api key",
        actualException.getMessage());
  }
}
