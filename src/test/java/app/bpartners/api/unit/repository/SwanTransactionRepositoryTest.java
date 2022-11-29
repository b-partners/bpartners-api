package app.bpartners.api.unit.repository;

import app.bpartners.api.endpoint.rest.security.principal.PrincipalProviderImpl;
import app.bpartners.api.endpoint.rest.security.swan.SwanConf;
import app.bpartners.api.manager.ProjectTokenManager;
import app.bpartners.api.repository.swan.SwanApi;
import app.bpartners.api.repository.swan.SwanCustomApi;
import app.bpartners.api.repository.swan.implementation.TransactionSwanRepositoryImpl;
import app.bpartners.api.repository.swan.model.Transaction;
import app.bpartners.api.repository.swan.response.TransactionResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static app.bpartners.api.integration.conf.TestUtils.API_URL;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.TestUtils.setUpProvider;
import static app.bpartners.api.integration.conf.TestUtils.swanTransaction1;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SwanTransactionRepositoryTest {
  PrincipalProviderImpl provider;
  SwanConf swanConf;
  ProjectTokenManager projectTokenManager;
  SwanCustomApi<TransactionResponse> swanCustomApi;
  SwanApi<TransactionResponse> swanApi;
  TransactionSwanRepositoryImpl transactionSwanRepository;

  @BeforeEach
  void setUp() {
    provider = mock(PrincipalProviderImpl.class);
    setUpProvider(provider);
    projectTokenManager = mock(ProjectTokenManager.class);
    swanConf = mock(SwanConf.class);
    swanCustomApi = mock(SwanCustomApi.class);
    swanApi = new SwanApi<>(provider, swanCustomApi, swanConf);
    when(swanConf.getApiUrl()).thenReturn(API_URL);
    when(swanCustomApi.getData(any(), any(), any())).thenReturn(transactionResponse());
    transactionSwanRepository =
        new TransactionSwanRepositoryImpl(projectTokenManager, swanConf, swanApi);
  }

  /*@Test
  void read_swan_transaction_by_identifier_ok() {
    Transaction actual = transactionSwanRepository.findById(SWAN_TRANSACTION_ID);

    assertNotNull(actual);
    assertEquals(swanTransaction1(), actual);
  }*/

  @Test
  void read_swan_transaction_by_accountId_ok() {
    List<Transaction> actual = transactionSwanRepository.getByIdAccount(JOE_DOE_ACCOUNT_ID);

    assertNotNull(actual);
  }

  private TransactionResponse transactionResponse() {
    return TransactionResponse.builder()
        .data(new TransactionResponse.Data(new TransactionResponse.Accounts(List.of(
            new TransactionResponse.Edge(new TransactionResponse.Node(
                new TransactionResponse.Transactions(List.of(swanTransaction1()))))))))
        .build();
  }
}
