package app.bpartners.api.unit.service;

import static app.bpartners.api.integration.conf.utils.TestUtils.joePersistedAccount;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.model.AccountStatus;
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

  @Test
  void initiate_account_validation_validation_required(){
    var account = mock(Account.class);

    when(repositoryMock.findById(any())).thenReturn(account);
    when(account.getStatus()).thenReturn(AccountStatus.VALIDATION_REQUIRED);
    when(bankRepositoryMock.initiateProValidation(any())).thenReturn("");

    assertNotNull(subject.initiateAccountValidation("accountId"));
  }

  @Test
  void initiate_account_validation_invalid_credentials(){
    var account = mock(Account.class);

    when(repositoryMock.findById(any())).thenReturn(account);
    when(account.getStatus()).thenReturn(AccountStatus.INVALID_CREDENTIALS);
    when(bankRepositoryMock.initiateBankConnectionEdition(any())).thenReturn("");

    assertNotNull(subject.initiateAccountValidation("accountId"));
  }

  @Test
  void initiate_account_validation_sca_required(){
    var account = mock(Account.class);

    when(repositoryMock.findById(any())).thenReturn(account);
    when(account.getStatus()).thenReturn(AccountStatus.SCA_REQUIRED);
    when(bankRepositoryMock.initiateScaSync(any())).thenReturn("");

    assertNotNull(subject.initiateAccountValidation("accountId"));
  }
}
