package app.bpartners.api.unit.repository;

import app.bpartners.api.endpoint.rest.security.principal.PrincipalProviderImpl;
import app.bpartners.api.endpoint.rest.security.swan.SwanConf;
import app.bpartners.api.repository.swan.SwanApi;
import app.bpartners.api.repository.swan.SwanCustomApi;
import app.bpartners.api.repository.swan.implementation.AccountSwanRepositoryImpl;
import app.bpartners.api.repository.swan.model.SwanAccount;
import app.bpartners.api.repository.swan.response.AccountResponse;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ID;
import static app.bpartners.api.integration.conf.TestUtils.joeDoeSwanAccount;
import static app.bpartners.api.integration.conf.TestUtils.setUpProvider;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SwanAccountRepositoryTest {
  SwanConf swanConf;
  PrincipalProviderImpl provider;
  SwanCustomApi<AccountResponse> swanCustomApi;
  SwanApi<AccountResponse> swanApi;
  AccountSwanRepositoryImpl accountSwanRepository;

  @BeforeEach
  void setUp() {
    provider = mock(PrincipalProviderImpl.class);
    setUpProvider(provider);
    swanConf = mock(SwanConf.class);
    swanCustomApi = mock(SwanCustomApi.class);
    swanApi = new SwanApi<>(provider, swanCustomApi, swanConf);
    when(swanCustomApi.getData(any(), any(), any())).thenReturn(accountResponse());
    accountSwanRepository = new AccountSwanRepositoryImpl(swanApi, swanCustomApi);
  }

  @Test
  void read_swan_account_by_user_ok() {
    List<SwanAccount> actual =
        accountSwanRepository.findByUserId(JOE_DOE_ID);

    assertNotNull(actual);
    assertThat(actual, is(not(empty())));
  }

  @Test
  void read_swan_account_by_identifier_ok() {
    List<SwanAccount> actual =
        accountSwanRepository.findById(JOE_DOE_ACCOUNT_ID);

    assertNotNull(actual);
    assertThat(actual, is(not(empty())));
  }

  private AccountResponse accountResponse() {
    return AccountResponse.builder()
        .data(new AccountResponse.Data(
            new AccountResponse.Accounts(List.of(new AccountResponse.Edge(joeDoeSwanAccount())))))
        .build();
  }
}
