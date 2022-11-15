package app.bpartners.api.unit.repository;

import app.bpartners.api.endpoint.rest.security.principal.PrincipalProviderImpl;
import app.bpartners.api.endpoint.rest.security.swan.SwanConf;
import app.bpartners.api.repository.swan.SwanApi;
import app.bpartners.api.repository.swan.SwanCustomApi;
import app.bpartners.api.repository.swan.implementation.AccountHolderSwanRepositoryImpl;
import app.bpartners.api.repository.swan.model.AccountHolder;
import app.bpartners.api.repository.swan.response.AccountHolderResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.TestUtils.SWAN_ACCOUNTHOLDER_ID;
import static app.bpartners.api.integration.conf.TestUtils.joeDoeSwanAccountHolder;
import static app.bpartners.api.integration.conf.TestUtils.setUpProvider;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SwanAccountHolderRepositoryTest {
  @Value("${test.user.access.token}")
  private static String bearer;
  PrincipalProviderImpl provider;
  SwanConf swanConf;
  SwanCustomApi<AccountHolderResponse> swanCustomApi;
  SwanApi<AccountHolderResponse> swanApi;
  AccountHolderSwanRepositoryImpl accountHolderSwanRepository;

  @BeforeEach
  void setUp() {
    provider = mock(PrincipalProviderImpl.class);
    setUpProvider(provider);
    swanConf = mock(SwanConf.class);
    swanCustomApi = mock(SwanCustomApi.class);
    when(swanCustomApi.getData(any(), any(), any())).thenReturn(accountHolderResponse());
    swanApi = new SwanApi<>(provider, swanCustomApi, swanConf);
    accountHolderSwanRepository = new AccountHolderSwanRepositoryImpl(swanApi, swanCustomApi);
  }

  @Test
  void read_swan_account_holder_by_identifier_ok() {
    AccountHolder actual =
        accountHolderSwanRepository.getById(SWAN_ACCOUNTHOLDER_ID);

    assertNotNull(actual);
  }

  @Test
  void read_swan_account_holder_by_bearer_and_accountId_ok() {
    List<AccountHolder> actual =
        accountHolderSwanRepository.findAllByBearerAndAccountId(bearer, JOE_DOE_ACCOUNT_ID);

    assertNotNull(actual);
    assertThat(actual, is(not(empty())));
  }

  @Test
  void read_swan_account_holder_by_accountId_ok() {
    List<AccountHolder> actual =
        accountHolderSwanRepository.findAllByAccountId(JOE_DOE_ACCOUNT_ID);

    assertNotNull(actual);
    assertThat(actual, is(not(empty())));
  }

  private AccountHolderResponse accountHolderResponse() {
    return AccountHolderResponse.builder()
        .data(new AccountHolderResponse.Data(new AccountHolderResponse.AccountHolders(
            List.of(new AccountHolderResponse.Edge(joeDoeSwanAccountHolder())))))
        .build();
  }
}
