package app.bpartners.api.integration;

import app.bpartners.api.endpoint.rest.api.PayingApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.Transaction;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.integration.conf.utils.TestUtils;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Testcontainers;

import static app.bpartners.api.integration.conf.utils.TestUtils.JANE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.TRANSACTION1_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.UNKNOWN_TRANSACTION_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.assertThrowsApiException;
import static app.bpartners.api.integration.conf.utils.TestUtils.assertThrowsForbiddenException;
import static app.bpartners.api.integration.conf.utils.TestUtils.restTransaction1;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpCognito;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpLegalFileRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Testcontainers
class TransactionIT extends MockedThirdParties {
  private ApiClient anApiClient() {
    return TestUtils.anApiClient(TestUtils.JOE_DOE_TOKEN, localPort);
  }

  @BeforeEach
  public void setUp() {
    setUpLegalFileRepository(legalFileRepositoryMock);
    setUpCognito(cognitoComponentMock);
    when(bridgeApi.findTransactionsUpdatedByToken(any())).thenReturn(List.of());
  }

  @Test
  void read_transactions_by_label() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    List<Transaction> actual =
        api.getTransactions(JOE_DOE_ACCOUNT_ID, "Création", null, null, null, null);

    assertEquals(1, actual.size());
    assertEquals(restTransaction1(), actual.get(0));
  }

  @Test
  void read_transactions_twice_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    List<Transaction> actual1 =
        api.getTransactions(
            JOE_DOE_ACCOUNT_ID,
            null,
            null,
            null,
            null,
            null);
    List<Transaction> actual2 =
        api.getTransactions(JOE_DOE_ACCOUNT_ID,
            null,
            null,
            null,
            null,
            null);

    assertEquals(2, actual1.size());
    assertEquals(actual1, actual2);
    // TODO : actual transactions contains rest resource
  }

  @Test
  void read_transaction_by_id_ok() throws ApiException {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    Transaction actual = api.getTransactionById(JOE_DOE_ACCOUNT_ID, TRANSACTION1_ID);

    assertEquals(restTransaction1(), actual);
  }

  @Test
  void read_transaction_by_id_ko() {
    ApiClient joeDoeClient = anApiClient();
    PayingApi api = new PayingApi(joeDoeClient);

    assertThrowsApiException(
        "{\"type\":\"404 NOT_FOUND\",\"message\":\""
            + "Transaction.unknown_transaction_id is not found.\"}",
        () -> api.getTransactionById(JOE_DOE_ACCOUNT_ID, UNKNOWN_TRANSACTION_ID));
    assertThrowsForbiddenException(() -> api.getTransactionById(JANE_ACCOUNT_ID, TRANSACTION1_ID));
  }
}
