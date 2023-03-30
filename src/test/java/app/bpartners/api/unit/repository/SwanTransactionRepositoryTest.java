package app.bpartners.api.unit.repository;

import app.bpartners.api.endpoint.rest.security.principal.PrincipalProviderImpl;
import app.bpartners.api.endpoint.rest.security.swan.SwanConf;
import app.bpartners.api.repository.swan.SwanCustomApi;
import app.bpartners.api.repository.swan.implementation.TransactionSwanRepositoryImpl;
import app.bpartners.api.repository.swan.model.SwanTransaction;
import app.bpartners.api.repository.swan.response.OneTransactionResponse;
import app.bpartners.api.repository.swan.response.TransactionResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedConstruction;

import static app.bpartners.api.integration.conf.TestUtils.API_URL;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.TestUtils.SWAN_TRANSACTION_ID;
import static app.bpartners.api.integration.conf.TestUtils.setUpProvider;
import static app.bpartners.api.integration.conf.TestUtils.swanTransaction1;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

class SwanTransactionRepositoryTest {
  public static final String ANY_BEARER_TOKEN = "any_bearer_token";
  PrincipalProviderImpl provider;
  SwanConf swanConf;
  SwanCustomApi<TransactionResponse> swanCustomApi;
  TransactionSwanRepositoryImpl transactionSwanRepository;

  @BeforeEach
  void setUp() {
    provider = mock(PrincipalProviderImpl.class);
    setUpProvider(provider);
    swanConf = mock(SwanConf.class);
    swanCustomApi = mock(SwanCustomApi.class);
    when(swanConf.getApiUrl()).thenReturn(API_URL);
    when(swanCustomApi.getData(any(), any(), any())).thenReturn(transactionResponse());
    transactionSwanRepository =
        new TransactionSwanRepositoryImpl(swanConf, swanCustomApi);
  }

  @Test
  void read_swan_transaction_by_identifier_ok() {
    MockedConstruction<OneTransactionResponse> mockedConstruction = mockConstruction(
        OneTransactionResponse.class, (mock, context) -> {
          when(mock.getData()).thenReturn(oneTransactionResponse().getData());
        });
    SwanTransaction actual = transactionSwanRepository.findById(
        SWAN_TRANSACTION_ID, ANY_BEARER_TOKEN);

    assertNotNull(actual);
    mockedConstruction.close();
  }

  @Test
  void read_swan_transaction_by_accountId_ok() {
    List<SwanTransaction> actual =
        transactionSwanRepository.getByIdAccount(JOE_DOE_ACCOUNT_ID, ANY_BEARER_TOKEN);

    assertNotNull(actual);
  }

  private TransactionResponse transactionResponse() {
    return TransactionResponse.builder()
        .data(
            new TransactionResponse.Data(
                new TransactionResponse.Account(
                    new TransactionResponse.Transactions(
                        List.of(swanTransaction1())
                    )
                )
            )
        )
        .build();
  }

  private OneTransactionResponse oneTransactionResponse() {
    return OneTransactionResponse.builder()
        .data(new OneTransactionResponse.Data(new SwanTransaction.Node()))
        .build();
  }
}
