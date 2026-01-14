package app.bpartners.api.service.user;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.api.model.UserAnalysisApiKey;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

class UserAnalysisApiKeyServiceTest {

  RestTemplate restTemplateMock = mock();
  UserAnalysisApiKeyService subject = new UserAnalysisApiKeyService("https://dum.my", restTemplateMock);

  @Test
  void getAnalysisApiKey_ok() {
    UserAnalysisApiKeyService.CreatedAnalysisApiKey createdApiKey =
        new UserAnalysisApiKeyService.CreatedAnalysisApiKey("apikey", Instant.now());
    UriComponentsBuilder uriBuilder =
        UriComponentsBuilder.fromHttpUrl("https://dum.my" + "/api/keys");

    when(restTemplateMock.postForObject(eq(uriBuilder.toUriString()), any(), any()))
        .thenReturn(createdApiKey);

    UserAnalysisApiKey actual = subject.getAnalysisApiKey(mock());

    assertEquals(createdApiKey.key(), actual.getApiKey());
    verify(restTemplateMock).postForObject(eq(uriBuilder.toUriString()), any(), any());
  }
}
