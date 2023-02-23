package app.bpartners.api.unit.repository;

import app.bpartners.api.endpoint.rest.model.AccountStatus;
import app.bpartners.api.model.Account;
import app.bpartners.api.model.User;
import app.bpartners.api.model.mapper.AccountMapper;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.implementation.AccountRepositoryImpl;
import app.bpartners.api.repository.jpa.AccountJpaRepository;
import app.bpartners.api.repository.jpa.model.HAccount;
import app.bpartners.api.repository.jpa.model.HUser;
import app.bpartners.api.repository.swan.AccountSwanRepository;
import app.bpartners.api.repository.swan.model.SwanAccount;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_ACCOUNT_ID;
import static app.bpartners.api.integration.conf.TestUtils.JOE_DOE_TOKEN;
import static app.bpartners.api.integration.conf.TestUtils.joeDoeSwanAccount;
import static app.bpartners.api.service.utils.FractionUtils.parseFraction;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AccountRepositoryImplTest {
  AccountSwanRepository accountSwanRepositoryMock;
  AccountJpaRepository accountJpaRepositoryMock;
  UserRepository userRepositoryMock;
  AccountMapper accountMapperMock;
  AccountRepositoryImpl accountRepository;

  @BeforeEach
  void setUp() {
    accountSwanRepositoryMock = mock(AccountSwanRepository.class);
    accountJpaRepositoryMock = mock(AccountJpaRepository.class);
    userRepositoryMock = mock(UserRepository.class);
    accountMapperMock = mock(AccountMapper.class);
    accountRepository =
        new AccountRepositoryImpl(accountSwanRepositoryMock, accountJpaRepositoryMock,
            userRepositoryMock, accountMapperMock);
    when(accountMapperMock.toDomain(any(SwanAccount.class), any())).thenReturn(domain());
    when(accountMapperMock.toDomain(any(HAccount.class), any())).thenReturn(domain());
    when(accountMapperMock.toDomain(any(SwanAccount.class), any(HAccount.class), any()))
        .thenReturn(domain());
    when(accountMapperMock.toEntity(any(Account.class))).thenReturn(new HAccount());
    when(userRepositoryMock.getUserByToken(any())).thenReturn(User.builder()
        .id("joe_doe_id")
        .account(Account.builder()
            .id("c15924bf-61f9-4381-8c9b-d34369bf91f7")
            .name("Account name")
            .userId("joe_doe_id")
            .build())
        .build());
  }

  @Test
  void read_swan_account_by_bearer_ok() {
    when(accountSwanRepositoryMock.findByBearer(any(String.class)))
        .thenReturn(List.of(joeDoeSwanAccount()));
    ArgumentCaptor<String> accountIdCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<List> toPersistCaptor = ArgumentCaptor.forClass(List.class);

    List<Account> actual = accountRepository.findByBearer(JOE_DOE_TOKEN);
    verify(accountJpaRepositoryMock).findById(accountIdCaptor.capture());
    verify(accountJpaRepositoryMock).saveAll(toPersistCaptor.capture());

    assertNotNull(actual);
    assertEquals(joeDoeSwanAccount().getId(), accountIdCaptor.getValue());
    assertNotNull(toPersistCaptor.getValue());
    assertEquals(HAccount.class, toPersistCaptor.getValue().get(0).getClass());
  }

  @Test
  void read_swan_account_by_bearer_then_user_id_ok() {
    when(accountSwanRepositoryMock.findByBearer(any(String.class)))
        .thenReturn(List.of());
    when(userRepositoryMock.getUserByToken(any(String.class))).thenReturn(authenticatedUser());
    when(accountJpaRepositoryMock.findById(any(String.class))).thenReturn(
        Optional.of(HAccount.builder()
            .id("c15924bf-61f9-4381-8c9b-d34369bf91f7")
            .name("Account name")
            .user(HUser.builder()
                .id("joe_doe_id")
                .build())
            .build()));
    ArgumentCaptor<String> bearerCaptor = ArgumentCaptor.forClass(String.class);
//    ArgumentCaptor<String> userIdCaptor = ArgumentCaptor.forClass(String.class);

    List<Account> actual = accountRepository.findByBearer(JOE_DOE_TOKEN);
    verify(userRepositoryMock).getUserByToken(bearerCaptor.capture());
//    verify(accountSwanRepositoryMock).findByUserId(userIdCaptor.capture());

    assertNotNull(actual);
    assertEquals(JOE_DOE_TOKEN, bearerCaptor.getValue());
//    assertEquals(authenticatedUser().getId(), userIdCaptor.getValue());
  }

  @Test
  void read_swan_accounts_by_identifier_ok() {
    when(accountSwanRepositoryMock.findByBearer(any(String.class)))
        .thenReturn(List.of());
    when(accountJpaRepositoryMock.findById(any(String.class))).thenReturn(
        Optional.of(entity()));
    ArgumentCaptor<String> accountIdCaptor = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<HAccount> accountCaptor = ArgumentCaptor.forClass(HAccount.class);

    Account actual = accountRepository.findById(JOE_DOE_ACCOUNT_ID);
    verify(accountJpaRepositoryMock).findById(accountIdCaptor.capture());
    verify(accountMapperMock).toDomain(accountCaptor.capture(), any());

    assertNotNull(actual);
    assertEquals(JOE_DOE_ACCOUNT_ID, accountIdCaptor.getValue());
    assertEquals(entity(), accountCaptor.getValue());
  }

  Account domain() {
    return Account.builder()
        .id(joeDoeSwanAccount().getId())
        .name(joeDoeSwanAccount().getName())
        .bic(joeDoeSwanAccount().getBic())
        .availableBalance(
            parseFraction(joeDoeSwanAccount().getBalances().getAvailable().getValue()))
        .status(AccountStatus.OPENED)
        .build();
  }

  User authenticatedUser() {
    return User.builder()
        .id("user_id")
        .account(Account.builder()
            .id("c15924bf-61f9-4381-8c9b-d34369bf91f7")
            .name("Account name")
            .userId("joe_doe_id")
            .build())
        .build();
  }

  HAccount entity() {
    return HAccount.builder()
        .id("c15924bf-61f9-4381-8c9b-d34369bf91f7")
        .name("Account name")
        .user(HUser.builder()
            .id("joe_doe_id")
            .build())
        .build();
  }
}
