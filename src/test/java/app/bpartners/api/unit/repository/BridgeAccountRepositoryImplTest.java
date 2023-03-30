package app.bpartners.api.unit.repository;

import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.endpoint.rest.security.model.Principal;
import app.bpartners.api.model.User;
import app.bpartners.api.repository.bridge.BridgeApi;
import app.bpartners.api.repository.bridge.model.Account.BridgeAccount;
import app.bpartners.api.repository.bridge.repository.implementation.BridgeAccountRepositoryImpl;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_TOKEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

class BridgeAccountRepositoryImplTest {
  private BridgeAccountRepositoryImpl bridgeAccountRepository;
  private BridgeApi bridgeApi;

  @BeforeEach
  void setUp() {
    bridgeApi = mock(BridgeApi.class);
    bridgeAccountRepository = new BridgeAccountRepositoryImpl(bridgeApi);


    when(bridgeApi.findAccountsByToken(JOE_DOE_TOKEN)).thenReturn(List.of(bridgeAccount()));
    when(bridgeApi.findByAccountById(any(), any())).thenReturn(bridgeAccount());
  }

  @Test
  void read_bridge_accounts_by_bearer_ok() {
    List<BridgeAccount> actual = bridgeAccountRepository.findByBearer(JOE_DOE_TOKEN);

    assertNotNull(actual);
    assertFalse(actual.isEmpty());

    reset(bridgeApi);
    when(bridgeApi.findAccountsByToken(eq(JOE_DOE_TOKEN))).thenReturn(List.of());

    List<BridgeAccount> actualEmpty = bridgeAccountRepository.findByBearer(JOE_DOE_TOKEN);

    assertNotNull(actualEmpty);
    assertTrue(actualEmpty.isEmpty());
  }

  @Test
  void read_bridge_accounts_by_user() {
    MockedStatic<AuthProvider> authProviderMockedStatic = Mockito.mockStatic(AuthProvider.class);
    authProviderMockedStatic.when(AuthProvider::getPrincipal).thenReturn(new Principal(new User()
        , JOE_DOE_TOKEN));
    List<BridgeAccount> actual = bridgeAccountRepository.findAllByAuthenticatedUser();

    assertNotNull(actual);
    assertFalse(actual.isEmpty());
  }

  @Test
  void read_bridge_accounts_by_id() {
    BridgeAccount actual = bridgeAccountRepository.findById(bridgeAccount().getId());

    assertNotNull(actual);
    assertEquals(bridgeAccount(), actual);
  }

  BridgeAccount bridgeAccount() {
    return BridgeAccount.builder()
        .id(1L)
        .name("bridge account")
        .build();
  }
}
