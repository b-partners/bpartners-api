package app.bpartners.api.service.user;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.OK;

import app.bpartners.api.model.User;
import app.bpartners.api.model.UserAnalysisApiKey;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

class UserAnalysisApiKeyServiceTest {

  RestTemplate restTemplateMock = mock();
  UserAnalysisApiKeyService subject =
      new UserAnalysisApiKeyService("https://dum.my", "dummy", restTemplateMock);

  @Test
  void getAnalysisApiKey_ok() {
    var createdApiKey =
        new UserAnalysisApiKeyService.CreatedAnalysisApiKey("apikey", Instant.now());
    var uriBuilder = UriComponentsBuilder.fromHttpUrl("https://dum.my" + "/api/keys");
    var responseEntityMock = mock(ResponseEntity.class);

    when(responseEntityMock.getStatusCode()).thenReturn(OK);
    when(restTemplateMock.exchange(
            eq(uriBuilder.toUriString()),
            eq(POST),
            any(HttpEntity.class),
            any(ParameterizedTypeReference.class)))
        .thenReturn(responseEntityMock);
    when(responseEntityMock.getBody()).thenReturn(List.of(createdApiKey));

    UserAnalysisApiKey actual = subject.getAnalysisApiKey(mock());

    assertEquals(createdApiKey.getKey(), actual.getApiKey());
  }

  @Test
  void throw_exception_when_api_exception_occurs() {
    var uriBuilder = UriComponentsBuilder.fromHttpUrl("https://dum.my" + "/api/keys");
    var responseEntityMock = mock(ResponseEntity.class);
    var userMock = mock(User.class);
    var userEmail = randomUUID() + "@email.com";

    when(userMock.getEmail()).thenReturn(userEmail);
    when(responseEntityMock.getStatusCode()).thenReturn(BAD_REQUEST);
    when(restTemplateMock.exchange(
            eq(uriBuilder.toUriString()),
            eq(POST),
            any(HttpEntity.class),
            any(ParameterizedTypeReference.class)))
        .thenReturn(responseEntityMock);

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
    var uriBuilder = UriComponentsBuilder.fromHttpUrl("https://dum.my" + "/api/keys");
    var responseEntityMock = mock(ResponseEntity.class);
    var userMock = mock(User.class);
    var userEmail = randomUUID() + "@email.com";

    when(userMock.getEmail()).thenReturn(userEmail);
    when(responseEntityMock.getStatusCode()).thenReturn(OK);
    when(responseEntityMock.getBody()).thenReturn(List.of());
    when(restTemplateMock.exchange(
            eq(uriBuilder.toUriString()),
            eq(POST),
            any(HttpEntity.class),
            any(ParameterizedTypeReference.class)))
        .thenReturn(responseEntityMock);

    var actualException =
        assertThrows(RuntimeException.class, () -> subject.getAnalysisApiKey(userMock));

    assertEquals(
        "API failed to create user.email=" + userEmail + " analysis api key",
        actualException.getMessage());
  }
}
