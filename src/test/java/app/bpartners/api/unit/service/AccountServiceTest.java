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
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.repository.*;
import app.bpartners.api.repository.bridge.BridgeApi;
import app.bpartners.api.repository.bridge.model.Item.BridgeItem;
import app.bpartners.api.service.AccountService;
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
  void save_ok() {
    when(repositoryMock.save((Account) any())).thenReturn(joePersistedAccount());

    assertEquals(joePersistedAccount(), subject.save(joePersistedAccount()));
  }

  @Test
  void update_account_identity_ok() {
    var account = mock(UpdateAccountIdentity.class);

    when(repositoryMock.save((UpdateAccountIdentity) any())).thenReturn(joePersistedAccount());

    assertEquals(joePersistedAccount(), subject.updateAccountIdentity(account));
  }

  @Test
  void initiate_account_validation_validation_required() {
    var account = mock(Account.class);

    when(repositoryMock.findById(any())).thenReturn(account);
    when(account.getStatus()).thenReturn(AccountStatus.VALIDATION_REQUIRED);
    when(bankRepositoryMock.initiateProValidation(any())).thenReturn("");

    assertNotNull(subject.initiateAccountValidation("accountId"));
  }

  @Test
  void initiate_account_validation_invalid_credentials() {
    var account = mock(Account.class);

    when(repositoryMock.findById(any())).thenReturn(account);
    when(account.getStatus()).thenReturn(AccountStatus.INVALID_CREDENTIALS);
    when(bankRepositoryMock.initiateBankConnectionEdition(any())).thenReturn("");

    assertNotNull(subject.initiateAccountValidation("accountId"));
  }

  @Test
  void initiate_account_validation_sca_required() {
    var account = mock(Account.class);

    when(repositoryMock.findById(any())).thenReturn(account);
    when(account.getStatus()).thenReturn(AccountStatus.SCA_REQUIRED);
    when(bankRepositoryMock.initiateScaSync(any())).thenReturn("");

    assertNotNull(subject.initiateAccountValidation("accountId"));
  }

  @Test
  void initiate_account_validation_default() {
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
  void initiate_bank_conneciton_throws_bad_request_exception() {
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
  void initiate_bank_connection_ok() {
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
  void disconnect_bank_not_implemented_exception() {
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
    when(accounts.get(0)).thenReturn(account); // Sélectionne le premier élément de la liste
    when(account.isEnabled()).thenReturn(true);
    when(account.isActive()).thenReturn(true);
    when(bridgeApiMock.findItemsByToken(any())).thenReturn(bridgeBankConnections);
    when(bridgeBankConnections.isEmpty()).thenReturn(false);
    when(bridgeBankConnections.get(0))
        .thenReturn(bridgeItem); // Sélectionne le premier élément de la liste
    when(bankRepositoryMock.disconnectBank(any(User.class))).thenReturn(false);

    assertThrows(ApiException.class, () -> subject.disconnectBank(USER1_ID));
  }
}
