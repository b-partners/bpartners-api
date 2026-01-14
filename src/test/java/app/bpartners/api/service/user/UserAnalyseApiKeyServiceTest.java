package app.bpartners.api.service.user;

import static app.bpartners.api.service.user.UserAnalyseApiKeyService.GEOJOBS_BASE_URL;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.api.model.UserAnalysisApiKey;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

class UserAnalyseApiKeyServiceTest {

  RestTemplate restTemplateMock = mock();
  UserAnalyseApiKeyService subject = new UserAnalyseApiKeyService(restTemplateMock);

  @Test
  void getAnalysisApiKey_ok() {
    UserAnalyseApiKeyService.CreatedAnalysisApiKey createdApiKey =
        new UserAnalyseApiKeyService.CreatedAnalysisApiKey("apikey", Instant.now());
    UriComponentsBuilder uriBuilder =
        UriComponentsBuilder.fromHttpUrl(GEOJOBS_BASE_URL + "/api/keys");

    when(restTemplateMock.postForObject(eq(uriBuilder.toUriString()), any(), any()))
        .thenReturn(createdApiKey);

    UserAnalysisApiKey actual = subject.getAnalysisApiKey(mock());

    assertEquals(createdApiKey.key(), actual.getApiKey());
    verify(restTemplateMock).postForObject(eq(uriBuilder.toUriString()), any(), any());
  }
}
