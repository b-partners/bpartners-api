package app.bpartners.api.integration;

import static app.bpartners.api.integration.conf.utils.TestUtils.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.api.endpoint.rest.api.SecurityApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.ApiKey;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.integration.conf.utils.TestUtils;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureMockMvc
@Slf4j
class ApiKeyIT extends MockedThirdParties {
  private ApiClient anApiClientWithBearer() {
    return TestUtils.anApiClient(JOE_DOE_TOKEN, null, localPort);
  }

  @BeforeEach
  public void setUp() {
    setUpLegalFileRepository(legalFileRepositoryMock);
    setUpCognito(cognitoComponentMock);
    setUpUserSubscription(subscriptionService);
  }

  @Test
  void find_user_api_key() throws ApiException {
    ApiClient client = anApiClientWithBearer();
    SecurityApi api = new SecurityApi(client);

    var actual = api.findApiKey();

    assertEquals(List.of(expected()), actual);
  }

  ApiKey expected() {
    return new ApiKey().apiKey("joe_doe_api_key").userId("joe_doe_id");
  }
}
