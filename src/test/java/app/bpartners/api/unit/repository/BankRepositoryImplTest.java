package app.bpartners.api.unit.repository;

import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_TOKEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.model.Account;
import app.bpartners.api.model.Bank;
import app.bpartners.api.model.BankConnection;
import app.bpartners.api.model.User;
import app.bpartners.api.model.UserToken;
import app.bpartners.api.model.mapper.BankMapper;
import app.bpartners.api.model.mapper.UserMapper;
import app.bpartners.api.repository.UserTokenRepository;
import app.bpartners.api.repository.bridge.model.Bank.BridgeBank;
import app.bpartners.api.repository.bridge.model.Item.BridgeConnectItem;
import app.bpartners.api.repository.bridge.model.Item.BridgeItem;
import app.bpartners.api.repository.bridge.repository.BridgeBankRepository;
import app.bpartners.api.repository.implementation.BankRepositoryImpl;
import app.bpartners.api.repository.jpa.AccountHolderJpaRepository;
import app.bpartners.api.repository.jpa.AccountJpaRepository;
import app.bpartners.api.repository.jpa.BankJpaRepository;
import app.bpartners.api.repository.jpa.UserJpaRepository;
import app.bpartners.api.repository.jpa.model.HBank;
import app.bpartners.api.repository.jpa.model.HUser;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BankRepositoryImplTest {
  private static final String REDIRECT_URL = "https://www.redirecturl.com";
  BridgeBankRepository bridgeBankRepositoryMock;
  UserJpaRepository userJpaRepositoryMock;
  UserMapper userMapperMock;
  BankMapper bankMapperMock;
  BankJpaRepository bankJpaRepositoryMock;
  UserTokenRepository userTokenRepositoryMock;
  AccountHolderJpaRepository holderJpaRepositoryMock;
  AccountJpaRepository accountJpaRepositoryMock;
  BankRepositoryImpl subject;

  @BeforeEach
  void setUp() {
    bridgeBankRepositoryMock = mock(BridgeBankRepository.class);
    userJpaRepositoryMock = mock(UserJpaRepository.class);
    userMapperMock = mock(UserMapper.class);
    bankMapperMock = mock(BankMapper.class);
    bankJpaRepositoryMock = mock(BankJpaRepository.class);
    userTokenRepositoryMock = mock(UserTokenRepository.class);
    holderJpaRepositoryMock = mock(AccountHolderJpaRepository.class);
    accountJpaRepositoryMock = mock(AccountJpaRepository.class);

    subject =
        new BankRepositoryImpl(
            bridgeBankRepositoryMock,
            userJpaRepositoryMock,
            userMapperMock,
            bankMapperMock,
            bankJpaRepositoryMock,
            userTokenRepositoryMock,
            holderJpaRepositoryMock,
            accountJpaRepositoryMock);

    when(bridgeBankRepositoryMock.findById(any())).thenReturn(BridgeBank.builder().id(1L).build());
    when(bankJpaRepositoryMock.findAllByExternalId(any())).thenReturn(List.of(bankEntity()));
    when(bankMapperMock.toDomain(any(), any())).thenReturn(bank());
    when(bankJpaRepositoryMock.findById(any())).thenReturn(Optional.of(bankEntity()));
    when(bridgeBankRepositoryMock.getItemStatusRefreshedAt(any(), any()))
        .thenReturn(Instant.now().minus(1L, ChronoUnit.HOURS));
    when(bridgeBankRepositoryMock.refreshBankConnection(any(), any())).thenReturn(REDIRECT_URL);
    when(bridgeBankRepositoryMock.getBridgeItems()).thenReturn(List.of(bridgeItem()));
    when(bridgeBankRepositoryMock.validateCurrentProItems(any())).thenReturn(connectItem());
    when(bridgeBankRepositoryMock.editItem(any())).thenReturn(connectItem());
    when(bridgeBankRepositoryMock.synchronizeSca(any())).thenReturn(connectItem());
    when(userJpaRepositoryMock.save(any())).thenReturn(userEntity());
    when(userJpaRepositoryMock.getById(any())).thenReturn(userEntity());
    when(userMapperMock.toDomain(any())).thenReturn(user());
    when(userTokenRepositoryMock.getLatestTokenByAccount(JOE_DOE_ACCOUNT_ID))
        .thenReturn(userToken());
  }

  BridgeItem bridgeItem() {
    return BridgeItem.builder().id(1L).bankId(Long.valueOf(bank().getId())).status(0).build();
  }

  BridgeConnectItem connectItem() {
    return BridgeConnectItem.builder().redirectUrl(REDIRECT_URL).build();
  }

  HBank bankEntity() {
    return HBank.builder().id(bank().getId()).build();
  }

  HUser userEntity() {
    return HUser.builder()
        .id(user().getId())
        .bridgeItemId(bridgeItem().getId())
        .bankConnectionStatus(BankConnection.BankConnectionStatus.OK)
        .bridgeItemUpdatedAt(Instant.now())
        .bridgeItemLastRefresh(Instant.now())
        .build();
  }

  User user() {
    return User.builder()
        .id(JOE_DOE_ID)
        .email("user@email.com")
        .bankConnectionId(bridgeItem().getId())
        .accounts(List.of(Account.builder().build()))
        .build();
  }

  Bank bank() {
    return Bank.builder().id("5").build();
  }

  BankConnection bankConnection() {
    return BankConnection.builder().bridgeId(bridgeItem().getId()).user(user()).build();
  }

  UserToken userToken() {
    return UserToken.builder().user(user()).accessToken(JOE_DOE_TOKEN).build();
  }

  @Test
  void initiate_bank_connection_ok() {
    when(bridgeBankRepositoryMock.initiateBankConnection(any())).thenReturn(REDIRECT_URL);

    String actual = subject.initiateConnection(user());

    assertEquals(REDIRECT_URL, actual);
  }

  @Test
  void find_external_id_ok() {
    Bank actual = subject.findByExternalId(bank().getId());
    Bank actualNull = subject.findByExternalId(null);

    assertEquals(bank(), actual);
    assertNull(actualNull);
  }

  @Test
  void read_bank_by_identifier_ok() {
    Bank actual = subject.findById(bank().getId());

    assertEquals(bank(), actual);
  }

  @Test
  void refresh_bank_connection_ok() {
    Instant actual = subject.refreshBankConnection(userToken());

    assertEquals(Instant.now().toString().substring(0, 9), actual.toString().substring(0, 9));
  }

  @Test
  void initiate_pro_account_validation() {
    String actual = subject.initiateProValidation(JOE_DOE_ACCOUNT_ID);

    assertEquals(REDIRECT_URL, actual);
  }

  @Test
  void initiate_bank_connection_edition() {
    String actual = subject.initiateBankConnectionEdition(user().getDefaultAccount());

    assertEquals(REDIRECT_URL, actual);
  }

  @Test
  void manage_strong_authentication() {
    String actual = subject.initiateScaSync(user().getDefaultAccount());

    assertEquals(REDIRECT_URL, actual);
  }
}
