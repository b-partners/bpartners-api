package app.bpartners.api.unit.service;

import static app.bpartners.api.integration.conf.utils.TestUtils.USER1_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.joePersistedAccount;
import static app.bpartners.api.repository.implementation.BankRepositoryImpl.ITEM_STATUS_OK;
import static app.bpartners.api.repository.implementation.BankRepositoryImpl.TRY_AGAIN;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.DisconnectionInitiated;
import app.bpartners.api.endpoint.rest.model.AccountStatus;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.model.Account;
import app.bpartners.api.model.UpdateAccountIdentity;
import app.bpartners.api.model.User;
import app.bpartners.api.model.UserToken;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.repository.*;
import app.bpartners.api.repository.bridge.BridgeApi;
import app.bpartners.api.repository.bridge.model.Item.BridgeItem;
import app.bpartners.api.service.AccountService;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class AccountServiceTest {
  AccountService subject;
  AccountRepository repositoryMock;
  BankRepository bankRepositoryMock;
  UserRepository userRepositoryMock;
  TransactionsSummaryRepository summaryRepositoryMock;
  DbTransactionRepository transactionRepositoryMock;
  BridgeApi bridgeApiMock;
  EventProducer<DisconnectionInitiated> eventProducerMock;

  @BeforeEach
  void setUp() {
    bankRepositoryMock = mock(BankRepository.class);
    userRepositoryMock = mock(UserRepository.class);
    summaryRepositoryMock = mock(TransactionsSummaryRepository.class);
    transactionRepositoryMock = mock(DbTransactionRepository.class);
    repositoryMock = mock(AccountRepository.class);
    bridgeApiMock = mock(BridgeApi.class);
    eventProducerMock = mock(EventProducer.class);
    subject =
        new AccountService(
            repositoryMock,
            bankRepositoryMock,
            userRepositoryMock,
            bridgeApiMock,
            eventProducerMock);
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
    var account = mock(Account.class);
    var redirectionUrl = "redirectionUrl";
    var accountBuilder = mock(Account.AccountBuilder.class);

    when(userRepositoryMock.getById(any())).thenReturn(user);
    when(repositoryMock.save(any(Account.class))).thenReturn(account);
    when(user.getAccounts()).thenReturn(List.of(account));
    when(user.getName()).thenReturn("user_name");
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

  User user() {
    return User.builder().id(USER1_ID).accessToken("dummy").accounts(List.of()).build();
  }

  @Test
  void disconnect_bank_ok() {
    when(userRepositoryMock.getById(any())).thenReturn(user());
    when(bridgeApiMock.findItemsByToken(any())).thenReturn(List.of(BridgeItem.builder().build()));
    when(bankRepositoryMock.disconnectBank(user())).thenReturn(true);
    var eventCaptor = ArgumentCaptor.forClass(List.class);

    var actual = subject.disconnectBank(USER1_ID);
    verify(eventProducerMock, times(1)).accept(eventCaptor.capture());
    var eventValue = eventCaptor.getValue().getFirst();

    assertEquals(new DisconnectionInitiated(USER1_ID), eventValue);
    assertNull(actual);
  }
}
