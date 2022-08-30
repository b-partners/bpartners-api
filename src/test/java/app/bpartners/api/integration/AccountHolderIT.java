package app.bpartners.api.integration;

import app.bpartners.api.SentryConf;
import app.bpartners.api.endpoint.rest.api.UserAccountsApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.AccountHolder;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = AccountHolderIT.ContextInitializer.class)
@AutoConfigureMockMvc
class AccountHolderIT {
  @MockBean
  private SentryConf sentryConf;
  @Value("${test.user.access.token}")
  private String bearerToken;

  private static ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token, ContextInitializer.SERVER_PORT);
  }

  AccountHolder joeDoeAccountHolder() {
    return new AccountHolder()
        .id("b33e6eb0-e262-4596-a91f-20c6a7bfd343")
        .name("NUMER")
        .address("6 RUE PAUL LANGEVIN")
        .city("FONTENAY-SOUS-BOIS")
        .country("FRA")
        .postalCode("94120");
  }

  @Test
  void read_account_holders_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient(bearerToken);
    UserAccountsApi api = new UserAccountsApi(joeDoeClient);

    List<AccountHolder> actual = api.getAccountHolders(TestUtils.JOE_DOE_ID, JOE_DOE_ACCOUNT_ID);

    assertTrue(actual.contains(joeDoeAccountHolder()));
  }

  static class ContextInitializer extends AbstractContextInitializer {
    public static final int SERVER_PORT = TestUtils.anAvailableRandomPort();

    @Override
    public int getServerPort() {
      return SERVER_PORT;
    }
  }
}
