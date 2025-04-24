package app.bpartners.api.integration;

import static app.bpartners.api.integration.conf.utils.TestUtils.*;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.api.AddressAutocompletionApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.AutoCompletePrediction;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.integration.conf.utils.TestUtils;
import app.bpartners.api.service.google.maps.AddressAutoCompleteService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureMockMvc
@Slf4j
class AddressAutocompletionControllerIT extends MockedThirdParties {
  @MockBean AddressAutoCompleteService autoCompleteServiceMock;

  private ApiClient anApiClientWithBearer() {
    return TestUtils.anApiClient(JOE_DOE_TOKEN, null, localPort);
  }

  private ApiClient anApiClientWithApiKey() {
    return TestUtils.anApiClient(null, JOE_DOE_API_KEY, localPort);
  }

  @BeforeEach
  public void setUp() {
    setUpLegalFileRepository(legalFileRepositoryMock);
    setUpCognito(cognitoComponentMock);
    setUpUserSubscription(subscriptionService);

    when(autoCompleteServiceMock.autoCompleteAddress(any(), any()))
        .thenReturn(List.of(new AutoCompletePrediction()));
  }

  @Test
  void whoami_with_bearer() throws ApiException {
    ApiClient client = anApiClientWithBearer();
    AddressAutocompletionApi api = new AddressAutocompletionApi(client);

    var actual =
        api.autoCompleteAddress(
            "12 Boulevard de la Croisette, 06400 Cannes", randomUUID().toString());

    assertFalse(actual.isEmpty());
  }

  @Test
  void whoami_with_api_key() throws ApiException {
    ApiClient client = anApiClientWithApiKey();
    AddressAutocompletionApi api = new AddressAutocompletionApi(client);

    var actual =
        api.autoCompleteAddress(
            "12 Boulevard de la Croisette, 06400 Cannes", randomUUID().toString());

    assertFalse(actual.isEmpty());
  }
}
