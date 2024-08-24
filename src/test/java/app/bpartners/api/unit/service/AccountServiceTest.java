package app.bpartners.api.unit.service;

import static app.bpartners.api.integration.conf.utils.TestUtils.joePersistedAccount;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.model.Account;
import app.bpartners.api.model.UpdateAccountIdentity;
import app.bpartners.api.repository.*;
import app.bpartners.api.repository.bridge.BridgeApi;
import app.bpartners.api.service.AccountService;
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
}
