package app.bpartners.api.integration;

import app.bpartners.api.endpoint.rest.api.PayingApi;
import app.bpartners.api.endpoint.rest.client.ApiClient;
import app.bpartners.api.endpoint.rest.client.ApiException;
import app.bpartners.api.endpoint.rest.model.Transaction;
import app.bpartners.api.integration.conf.DbEnvContextInitializer;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.integration.conf.utils.TestUtils;
import app.bpartners.api.repository.bridge.repository.BridgeTransactionRepository;
import app.bpartners.api.repository.jpa.TransactionJpaRepository;
import app.bpartners.api.repository.jpa.model.HTransaction;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

import static app.bpartners.api.integration.conf.utils.TestUtils.JANE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_TOKEN;
import static app.bpartners.api.integration.conf.utils.TestUtils.TRANSACTION1_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.UNKNOWN_TRANSACTION_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.assertThrowsApiException;
import static app.bpartners.api.integration.conf.utils.TestUtils.assertThrowsForbiddenException;
import static app.bpartners.api.integration.conf.utils.TestUtils.restTransaction1;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpCognito;
import static app.bpartners.api.integration.conf.utils.TestUtils.setUpLegalFileRepository;
import static app.bpartners.api.integration.conf.utils.TransactionTestUtils.jpaTransactionEntity1;
import static app.bpartners.api.integration.conf.utils.TransactionTestUtils.transactionEntity1;
import static app.bpartners.api.integration.conf.utils.TransactionTestUtils.transactionEntity2;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
@ContextConfiguration(initializers = TransactionIT.ContextInitializer.class)
class TransactionIT extends MockedThirdParties {
  @MockBean
  private TransactionJpaRepository transactionJpaRepositoryMock;
  @MockBean
  private BridgeTransactionRepository bridgeTransactionRepositoryMock;

  private static ApiClient anApiClient(String token) {
    return TestUtils.anApiClient(token,
        DbEnvContextInitializer.getHttpServerPort());
  }

  @BeforeEach
  public void setUp() {
    setUpLegalFileRepository(legalFileRepositoryMock);
    setUpCognito(cognitoComponentMock);
    when(bridgeApi.findTransactionsUpdatedByToken(any()))
        .thenReturn(List.of());
  }

  @Test
  void read_transactions_twice_ok() throws ApiException {
    reset(transactionJpaRepositoryMock);
    List<HTransaction> mockedBridgeTransactions = List.of(
        transactionEntity1(), transactionEntity2());
    when(transactionJpaRepositoryMock.findByIdAccountOrderByPaymentDateTimeDesc(JOE_DOE_ACCOUNT_ID,
        PageRequest.of(0, 30)))
        .thenReturn(mockedBridgeTransactions);
    ApiClient joeDoeClient = anApiClient(JOE_DOE_TOKEN);
    PayingApi api = new PayingApi(joeDoeClient);

    List<Transaction> actual1 = api.getTransactions(JOE_DOE_ACCOUNT_ID, null, null);
    List<Transaction> actual2 = api.getTransactions(JOE_DOE_ACCOUNT_ID, null, null);

    assertEquals(2, actual1.size());
    assertEquals(actual1, actual2);
    //TODO : actual transactions contains rest resource
  }

  @Test
  void read_transaction_by_id_ok() throws ApiException {
    reset(transactionJpaRepositoryMock);
    when(transactionJpaRepositoryMock.findById(jpaTransactionEntity1().getId())).thenReturn(
        Optional.of(jpaTransactionEntity1()));
    ApiClient joeDoeClient = anApiClient(JOE_DOE_TOKEN);
    PayingApi api = new PayingApi(joeDoeClient);

    Transaction actual = api.getTransactionById(JOE_DOE_ACCOUNT_ID, TRANSACTION1_ID);

    assertEquals(restTransaction1(), actual);
  }

  @Test
  void read_transaction_by_id_ko() {
    ApiClient joeDoeClient = anApiClient(JOE_DOE_TOKEN);
    PayingApi api = new PayingApi(joeDoeClient);

    assertThrowsApiException("{\"type\":\"404 NOT_FOUND\",\"message\":\""
            + "Transaction.unknown_transaction_id is not found.\"}",
        () -> api.getTransactionById(JOE_DOE_ACCOUNT_ID, UNKNOWN_TRANSACTION_ID));
    assertThrowsForbiddenException(
        () -> api.getTransactionById(JANE_ACCOUNT_ID, TRANSACTION1_ID));
  }

  static class ContextInitializer extends DbEnvContextInitializer {
  }
}
