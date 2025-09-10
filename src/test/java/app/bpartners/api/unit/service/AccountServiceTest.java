package app.bpartners.api.unit.service;

import static app.bpartners.api.endpoint.rest.model.AccountStatus.OPENED;
import static app.bpartners.api.endpoint.rest.model.EnableStatus.ENABLED;
import static app.bpartners.api.integration.conf.utils.TestUtils.joePersistedAccount;
import static app.bpartners.api.service.account.AccountService.resetDefaultAccount;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.DisconnectionInitiated;
import app.bpartners.api.endpoint.rest.model.EnableStatus;
import app.bpartners.api.model.Account;
import app.bpartners.api.model.Money;
import app.bpartners.api.model.UpdateAccountIdentity;
import app.bpartners.api.model.User;
import app.bpartners.api.repository.*;
import app.bpartners.api.service.account.AccountService;
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
  EventProducer<DisconnectionInitiated> eventProducerMock;

  @BeforeEach
  void setUp() {
    bankRepositoryMock = mock(BankRepository.class);
    userRepositoryMock = mock(UserRepository.class);
    summaryRepositoryMock = mock(TransactionsSummaryRepository.class);
    transactionRepositoryMock = mock(DbTransactionRepository.class);
    repositoryMock = mock(AccountRepository.class);
    eventProducerMock = mock(EventProducer.class);
    subject =
        new AccountService(
            repositoryMock, bankRepositoryMock, userRepositoryMock, eventProducerMock);
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
}
