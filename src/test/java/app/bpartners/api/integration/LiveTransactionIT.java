package app.bpartners.api.integration;

import app.bpartners.api.endpoint.rest.api.PayingApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.Transaction;
import app.bpartners.api.endpoint.rest.model.TransactionStatus;
import app.bpartners.api.integration.conf.DbEnvContextInitializer;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.integration.conf.utils.TestUtils;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.restTransaction1;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpCognito;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpLegalFileRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@Testcontainers
class LiveTransactionIT extends MockedThirdParties {

  private ApiClient anApiClient() {
    return TestUtils.anApiClient(TestUtils.JOE_DOE_TOKEN, localPort);
  }

  @BeforeEach
  public void setUp() {
    setUpLegalFileRepository(legalFileRepositoryMock);
    setUpCognito(cognitoComponentMock);
    when(bridgeApi.findTransactionsUpdatedByToken(any()))
        .thenReturn(List.of());
  }

  @Test
  void read_transactions_by_label() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    List<Transaction> actual = api.getTransactions(JOE_DOE_ACCOUNT_ID, "Création", null, null,
        null, null);

    assertEquals(1, actual.size());
    assertEquals(restTransaction1(), actual.get(0));
  }

  @Test
  void read_filtered_transaction_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    List<Transaction> actualFilteredByStatus =
        api.getTransactions(JOE_DOE_ACCOUNT_ID, null, TransactionStatus.PENDING,
            null, null, null);
    List<Transaction> actualFilteredByCategory = api.getTransactions(JOE_DOE_ACCOUNT_ID, null, null,
        "Sponsoring", null, null);

    assertEquals(1, actualFilteredByStatus.size());
    assertEquals(1, actualFilteredByCategory.size());
    assertTrue(actualFilteredByStatus.contains(restTransaction1()));
  }
}
