package app.bpartners.api.service.user.analysis;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.ApiException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

class AnalysisApiKeyApiTest {
  public static final String ANALYSIS_API_BASE_URL = "http://geo-jobs.local";
  public static final String API_KEY_OPERATION_PATH = "/api/keys";
  public static final String ANALYSIS_ADMIN_KEY = "admin-key";

  RestTemplate restTemplate = mock();
  AnalysisApiKeyApi subject =
      new AnalysisApiKeyApi(ANALYSIS_API_BASE_URL, ANALYSIS_ADMIN_KEY, restTemplate);

  @Test
  void requestAnalysisApiKeys_sends_post_request_with_expected_body() throws Exception {
    var user = User.builder().email("john@doe.com").apiKey(randomUUID().toString()).build();
    when(restTemplate.exchange(
            eq(ANALYSIS_API_BASE_URL + API_KEY_OPERATION_PATH),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            any(ParameterizedTypeReference.class)))
        .thenReturn(ResponseEntity.ok(List.of(new CreatedAnalysisApiKey("key", null))));

    var response = subject.createAnalysisApiKeys(user);

    assertFalse(response.isEmpty());
    verify(restTemplate)
        .exchange(
            eq(ANALYSIS_API_BASE_URL + API_KEY_OPERATION_PATH),
            eq(HttpMethod.POST),
            argThat(
                request -> {
                  var headers = request.getHeaders();
                  return ANALYSIS_ADMIN_KEY.equals(headers.getFirst("x-api-key"));
                }),
            any(ParameterizedTypeReference.class));
  }

  @Test
  void requestAnalysisApiKeys_sends_user_dashboard_api_key() throws Exception {
    var dashboardApiKey = randomUUID().toString();
    var user = User.builder().email("john@doe.com").apiKey(dashboardApiKey).build();
    when(restTemplate.exchange(
            eq(ANALYSIS_API_BASE_URL + API_KEY_OPERATION_PATH),
            eq(HttpMethod.POST),
            any(HttpEntity.class),
            any(ParameterizedTypeReference.class)))
        .thenReturn(ResponseEntity.ok(List.of(new CreatedAnalysisApiKey("key", null))));
    var requestCaptor = ArgumentCaptor.forClass(HttpEntity.class);

    subject.createAnalysisApiKeys(user);

    verify(restTemplate)
        .exchange(
            eq(ANALYSIS_API_BASE_URL + API_KEY_OPERATION_PATH),
            eq(HttpMethod.POST),
            requestCaptor.capture(),
            any(ParameterizedTypeReference.class));
    var sentKeyCreations = (List<AnalysisApiKeyCreation>) requestCaptor.getValue().getBody();
    assertEquals(1, sentKeyCreations.size());
    assertEquals(dashboardApiKey, sentKeyCreations.getFirst().dashboardApiKey());
    assertEquals(user.getEmail(), sentKeyCreations.getFirst().consumerEmail());
  }

  @Test
  void requestAnalysisApiKeys_throws_when_user_has_no_dashboard_api_key() {
    var userId = randomUUID().toString();
    var user = User.builder().id(userId).email("john@doe.com").apiKey(null).build();

    var actualException =
        assertThrows(ApiException.class, () -> subject.createAnalysisApiKeys(user));

    assertEquals(
        "User(id=" + userId + ") has no dashboard api key to associate to its analysis api key",
        actualException.getMessage());
    verifyNoInteractions(restTemplate);
  }

  @Test
  void requestAnalysisApiKeyRevocation_sends_delete_request_with_expected_body() throws Exception {
    String apiKeyToRevoke = "key-to-revoke";
    when(restTemplate.exchange(
            eq(ANALYSIS_API_BASE_URL + API_KEY_OPERATION_PATH),
            eq(HttpMethod.DELETE),
            any(HttpEntity.class),
            any(ParameterizedTypeReference.class)))
        .thenReturn(ResponseEntity.ok(mock(RevokedAnalysisApiKey.class)));

    var response = subject.requestAnalysisApiKeyRevocation(apiKeyToRevoke);

    assertNotNull(response);
    verify(restTemplate)
        .exchange(
            eq(ANALYSIS_API_BASE_URL + API_KEY_OPERATION_PATH),
            eq(HttpMethod.DELETE),
            argThat(
                request -> {
                  var headers = request.getHeaders();
                  var body = (AnalysisApiKeyRevocation) request.getBody();
                  return ANALYSIS_ADMIN_KEY.equals(headers.getFirst("x-api-key"))
                      && apiKeyToRevoke.equals(body.keyValue());
                }),
            any(ParameterizedTypeReference.class));
  }
}
