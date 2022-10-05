package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.rest.api.ProspectingApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.Marketplace;
import app.bpartners.api.integration.conf.AbstractContextInitializer;
import app.bpartners.api.integration.conf.TestUtils;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.TestUtils.MARKETPLACE1_ID;
import static app.bpartners.api.integration.conf.TestUtils.MARKETPLACE2_ID;
import static app.bpartners.api.integration.conf.TestUtils.assertThrowsForbiddenException;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = MarketPlaceIT.ContextInitializer.class)
@AutoConfigureMockMvc
class MarketPlaceIT {
  @MockBean
  private SentryConf sentryConf;
  @Value("${test.user.access.token}")
  private String bearerToken;

  private static ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, ContextInitializer.SERVER_PORT);
  }

  Marketplace marketPlace1() {
    return new Marketplace()
        .id(MARKETPLACE1_ID)
        .name("marketplace1_name")
        .description("marketplace1_description")
        .websiteUrl("website URL")
        .logoUrl("logo URL");
  }

  Marketplace marketPlace2() {
    return new Marketplace()
        .id(MARKETPLACE2_ID)
        .name("marketplace2_name")
        .description("marketplace2_description")
        .websiteUrl("website URL")
        .logoUrl("logo URL");
  }

  @Test
  void read_marketplaces_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient(bearerToken);
    ProspectingApi api = new ProspectingApi(joeDoeClient);

    List<Marketplace> actual = api.getMarketplaces(JOE_DOE_ACCOUNT_ID);

    assertTrue(actual.contains(marketPlace1()));
    assertTrue(actual.contains(marketPlace2()));
  }

  @Test
  void read_marketplaces_ko() throws ApiException {
    ApiClient joeDoeClient = anApiClient(bearerToken);
    ProspectingApi api = new ProspectingApi(joeDoeClient);

    assertThrowsForbiddenException(() -> api.getMarketplaces("notjoeDoe accountId"));
  }

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
