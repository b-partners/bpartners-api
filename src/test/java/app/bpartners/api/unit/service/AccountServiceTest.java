package app.bpartners.api.unit.service;

import static app.bpartners.api.endpoint.rest.model.AccountStatus.OPENED;
import static app.bpartners.api.endpoint.rest.model.EnableStatus.ENABLED;
import static app.bpartners.api.integration.conf.utils.TestUtils.USER1_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.joePersistedAccount;
import static app.bpartners.api.repository.implementation.BankRepositoryImpl.ITEM_STATUS_OK;
import static app.bpartners.api.repository.implementation.BankRepositoryImpl.TRY_AGAIN;
import static app.bpartners.api.service.account.AccountService.resetDefaultAccount;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.DisconnectionInitiated;
import app.bpartners.api.endpoint.rest.model.AccountStatus;
import app.bpartners.api.endpoint.rest.model.EnableStatus;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.model.Account;
import app.bpartners.api.model.Money;
import app.bpartners.api.model.UpdateAccountIdentity;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.repository.*;
import app.bpartners.api.repository.bridge.BridgeApi;
import app.bpartners.api.repository.bridge.model.Item.BridgeItem;
import app.bpartners.api.service.account.AccountService;
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
  void find_all_active_accounts() {
    var preferredAccountId = "preferredAccountId";
    var account = Account.builder().id(preferredAccountId).enableStatus(ENABLED).build();
    var user =
        User.builder().accounts(List.of(account)).preferredAccountId(preferredAccountId).build();
    when(userRepositoryMock.findAll()).thenReturn(List.of(user));

    var actual = subject.findAllActiveAccounts();

    assertEquals(List.of(account), actual);
  }

  @Test
  void save_ok() {
    when(repositoryMock.save((Account) any())).thenReturn(joePersistedAccount());

    assertEquals(joePersistedAccount(), subject.save(joePersistedAccount()));
  }

  @Test
  void reset_default_account() {
    var user = User.builder().id("userId").firstName("firstName").lastName("lastName").build();
    var defaultAccount = Account.builder().build();

    var actual = resetDefaultAccount(user, defaultAccount);

    var expected =
        defaultAccount.toBuilder()
            .id(actual.getId())
            .name(user.getName())
            .userId(user.getId())
            .bic(null)
            .iban(null)
            .externalId(null)
            .bank(null)
            .externalId(null)
            .availableBalance(new Money())
            .status(OPENED)
            .enableStatus(EnableStatus.ENABLED)
            .build();
    assertEquals(expected, actual);
  }

  @Test
  void update_account_identity_ok() {
    var account = mock(UpdateAccountIdentity.class);

    when(repositoryMock.save((UpdateAccountIdentity) any())).thenReturn(joePersistedAccount());

    assertEquals(joePersistedAccount(), subject.updateAccountIdentity(account));
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
