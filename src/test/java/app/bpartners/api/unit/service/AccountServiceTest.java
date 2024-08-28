package app.bpartners.api.unit.service;

import static app.bpartners.api.integration.conf.utils.TestUtils.USER1_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.joePersistedAccount;
import static app.bpartners.api.repository.implementation.BankRepositoryImpl.ITEM_STATUS_OK;
import static app.bpartners.api.repository.implementation.BankRepositoryImpl.TRY_AGAIN;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.rest.model.AccountStatus;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.model.Account;
import app.bpartners.api.model.UpdateAccountIdentity;
import app.bpartners.api.model.User;
import app.bpartners.api.model.UserToken;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.repository.*;
import app.bpartners.api.repository.bridge.BridgeApi;
import app.bpartners.api.repository.bridge.model.Item.BridgeItem;
import app.bpartners.api.service.AccountService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AccountServiceTest {
  AccountService subject;
  AccountRepository repositoryMock;
  BankRepository bankRepositoryMock;
  UserRepository userRepositoryMock;
  TransactionsSummaryRepository summaryRepositoryMock;
  DbTransactionRepository transactionRepositoryMock;
  BridgeApi bridgeApiMock;

  @BeforeEach
  void setUp() {
    bankRepositoryMock = mock(BankRepository.class);
    userRepositoryMock = mock(UserRepository.class);
    summaryRepositoryMock = mock(TransactionsSummaryRepository.class);
    transactionRepositoryMock = mock(DbTransactionRepository.class);
    repositoryMock = mock(AccountRepository.class);
    bridgeApiMock = mock(BridgeApi.class);
    subject =
        new AccountService(
            repositoryMock,
            bankRepositoryMock,
            userRepositoryMock,
            summaryRepositoryMock,
            transactionRepositoryMock,
            bridgeApiMock);
  }

  @Test
  void findAllActiveAccountsOk() {
    var user1 = mock(User.class);
    var user2 = mock(User.class);
    var account1 = mock(Account.class);
    var account2 = mock(Account.class);

    when(user1.getDefaultAccount()).thenReturn(account1);
    when(user2.getDefaultAccount()).thenReturn(account2);
    when(userRepositoryMock.findAll()).thenReturn(List.of(user1, user2));

    List<Account> activeAccounts = subject.findAllActiveAccounts();

    assertEquals(2, activeAccounts.size());
    assertTrue(activeAccounts.contains(account1));
    assertTrue(activeAccounts.contains(account2));
    verify(userRepositoryMock, times(1)).findAll();
  }

  @Test
  void findAllActiveAccountsNoActiveAccounts() {
    when(userRepositoryMock.findAll()).thenReturn(new ArrayList<>());

    List<Account> activeAccounts = subject.findAllActiveAccounts();

    assertTrue(activeAccounts.isEmpty());
    verify(userRepositoryMock, times(1)).findAll();
  }

  @Test
  void refreshBankConnectionOk() {
    var userToken = mock(UserToken.class);
    var expectedTime = Instant.now();

    when(bankRepositoryMock.refreshBankConnection(any(UserToken.class))).thenReturn(expectedTime);

    var actualTime = subject.refreshBankConnection(userToken);
    assertEquals(expectedTime, actualTime);
    verify(bankRepositoryMock, times(1)).refreshBankConnection(userToken);
  }

  @Test
  void saveOk() {
    when(repositoryMock.save((Account) any())).thenReturn(joePersistedAccount());

    assertEquals(joePersistedAccount(), subject.save(joePersistedAccount()));
  }

  @Test
  void updateAccountIdentityOk() {
    var account = mock(UpdateAccountIdentity.class);

    when(repositoryMock.save((UpdateAccountIdentity) any())).thenReturn(joePersistedAccount());

    assertEquals(joePersistedAccount(), subject.updateAccountIdentity(account));
  }

  @Test
  void initiateAccountValidationValidationRequired() {
    var account = mock(Account.class);

    when(repositoryMock.findById(any())).thenReturn(account);
    when(account.getStatus()).thenReturn(AccountStatus.VALIDATION_REQUIRED);
    when(bankRepositoryMock.initiateProValidation(any())).thenReturn("");

    assertNotNull(subject.initiateAccountValidation("accountId"));
  }

  @Test
  void initiateAccountValidationInvalidCredentials() {
    var account = mock(Account.class);

    when(repositoryMock.findById(any())).thenReturn(account);
    when(account.getStatus()).thenReturn(AccountStatus.INVALID_CREDENTIALS);
    when(bankRepositoryMock.initiateBankConnectionEdition(any())).thenReturn("");

    assertNotNull(subject.initiateAccountValidation("accountId"));
  }

  @Test
  void initiateAccountValidationScaRequired() {
    var account = mock(Account.class);

    when(repositoryMock.findById(any())).thenReturn(account);
    when(account.getStatus()).thenReturn(AccountStatus.SCA_REQUIRED);
    when(bankRepositoryMock.initiateScaSync(any())).thenReturn("");

    assertNotNull(subject.initiateAccountValidation("accountId"));
  }

  @Test
  void initiateAccountValidationDefault() {
    var account = mock(Account.class);

    when(repositoryMock.findById(any())).thenReturn(account);
    when(account.getStatus()).thenReturn(AccountStatus.OPENED);

    assertThrows(
        BadRequestException.class,
        () -> {
          subject.initiateAccountValidation("accountId");
        });
  }

  @Test
  void initiateBankConnecitonThrowsBadRequestException() {
    var urls = mock(RedirectionStatusUrls.class);
    var user = mock(User.class);
    var accounts = mock(List.class);
    var account = mock(Account.class);

    when(userRepositoryMock.getById(any())).thenReturn(user);
    when(user.getAccounts()).thenReturn(accounts);
    when(user.getName()).thenReturn("user_name");
    when(accounts.get(anyInt())).thenReturn(account);
    when(user.getDefaultAccount()).thenReturn(account);
    when(user.getBankConnectionId()).thenReturn((long) ITEM_STATUS_OK);

    assertThrows(
        BadRequestException.class,
        () -> {
          subject.initiateBankConnection(USER1_ID, urls);
        });
  }

  @Test
  void initiateBankConnectionOk() {
    var urls = mock(RedirectionStatusUrls.class);
    var user = mock(User.class);
    var accounts = mock(List.class);
    var account = mock(Account.class);
    var redirectionUrl = "redirectionUrl";
    var accountBuilder = mock(Account.AccountBuilder.class);

    when(userRepositoryMock.getById(any())).thenReturn(user);
    when(repositoryMock.save(any(Account.class))).thenReturn(account);
    when(user.getAccounts()).thenReturn(accounts);
    when(user.getName()).thenReturn("user_name");
    when(accounts.get(anyInt())).thenReturn(account);
    when(user.getDefaultAccount()).thenReturn(account);
    when(user.getBankConnectionId()).thenReturn((long) TRY_AGAIN);
    when(bankRepositoryMock.initiateConnection(any(User.class))).thenReturn(redirectionUrl);
    when(account.toBuilder()).thenReturn(accountBuilder);
    when(accountBuilder.userId(any())).thenReturn(accountBuilder);
    when(accountBuilder.bank(any())).thenReturn(accountBuilder);
    when(accountBuilder.bic(any())).thenReturn(accountBuilder);
    when(accountBuilder.iban(any())).thenReturn(accountBuilder);
    when(accountBuilder.externalId(any())).thenReturn(accountBuilder);

    var actual = subject.initiateBankConnection(USER1_ID, urls);
    assertEquals(redirectionUrl, actual.getRedirectionUrl());
    assertEquals(urls, actual.getRedirectionStatusUrls());
  }

  @Test
  void disconnectBankNotImplementedException() {
    var user = mock(User.class);
    var accounts = mock(List.class);
    var account = mock(Account.class);
    var bridgeBankConnections = mock(List.class);

    when(userRepositoryMock.getById(any())).thenReturn(user);
    when(repositoryMock.findByUserId(any())).thenReturn(accounts);
    when(accounts.get(anyInt())).thenReturn(account);
    when(account.isEnabled()).thenReturn(true);
    when(account.isActive()).thenReturn(true);
    when(bridgeApiMock.findItemsByToken(any())).thenReturn(bridgeBankConnections);
    when(bridgeBankConnections.isEmpty()).thenReturn(true);

    assertThrows(
        NotImplementedException.class,
        () -> {
          subject.disconnectBank(USER1_ID);
        });
  }

  @Test
  void disconnectBankThrowsApiExceptionWhenDisconnectFails() {
    var user = mock(User.class);
    var accounts = mock(List.class);
    var account = mock(Account.class);
    var bridgeBankConnections = mock(List.class);
    var bridgeItem = mock(BridgeItem.class);

    when(userRepositoryMock.getById(any())).thenReturn(user);
    when(repositoryMock.findByUserId(any())).thenReturn(accounts);
    when(accounts.get(0)).thenReturn(account);
    when(account.isEnabled()).thenReturn(true);
    when(account.isActive()).thenReturn(true);
    when(bridgeApiMock.findItemsByToken(any())).thenReturn(bridgeBankConnections);
    when(bridgeBankConnections.isEmpty()).thenReturn(false);
    when(bridgeBankConnections.get(0)).thenReturn(bridgeItem);
    when(bankRepositoryMock.disconnectBank(any(User.class))).thenReturn(false);

    assertThrows(ApiException.class, () -> subject.disconnectBank(USER1_ID));
  }
}
