package app.bpartners.api.unit.repository;

import app.bpartners.api.model.User;
import app.bpartners.api.model.UserToken;
import app.bpartners.api.repository.UserTokenRepository;
import app.bpartners.api.repository.bridge.BridgeApi;
import app.bpartners.api.repository.bridge.model.Bank.BridgeBank;
import app.bpartners.api.repository.bridge.repository.implementation.BridgeBankRepositoryImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_TOKEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

class BridgeBankRepositoryImplTest {
  private final Long BRIDGE_BANK_ID = 1L;
  private BridgeBankRepositoryImpl bridgeBankRepository;
  private BridgeApi bridgeApi;
  private UserTokenRepository userTokenRepository;

  @BeforeEach
  void setUp() {
    bridgeApi = mock(BridgeApi.class);
    userTokenRepository = mock(UserTokenRepository.class);
    bridgeBankRepository = new BridgeBankRepositoryImpl(bridgeApi, userTokenRepository);

    when(bridgeApi.findBankById(any(Long.class))).thenReturn(bridgeBank());
    when(bridgeApi.initiateBankConnection(any(), any())).thenReturn(JOE_DOE_TOKEN);
    when(userTokenRepository.getLatestTokenByUser(any(User.class))).thenReturn(UserToken.builder()
        .accessToken(JOE_DOE_TOKEN)
        .build());
  }

  @Test
  void read_bridge_bank_by_id_ok() {
    BridgeBank actual = bridgeBankRepository.findById(BRIDGE_BANK_ID);

    assertNotNull(actual);
    assertEquals(bridgeBank(), actual);
  }

  @Test
  void read_bridge_bank_by_id_ko() {
    reset(bridgeApi);
    when(bridgeApi.findBankById(any(Long.class))).thenReturn(null);

    BridgeBank actual = bridgeBankRepository.findById(BRIDGE_BANK_ID);

    assertNull(actual);
  }

  /*@Test
  void initiate_bank_connection() {
    MockedStatic<AuthProvider> authProviderMockedStatic = Mockito.mockStatic(AuthProvider.class);
    authProviderMockedStatic.when(AuthProvider::getPrincipal).thenReturn(new Principal(new User()
        , JOE_DOE_TOKEN));
    String actual = bridgeBankRepository.initiateBankConnection(VALID_EMAIL);

    assertNotNull(actual);
    assertEquals(JOE_DOE_TOKEN, actual);
  }*/

  BridgeBank bridgeBank() {
    return BridgeBank.builder()
        .id(1L)
        .name("bridge bank")
        .build();
  }
}
