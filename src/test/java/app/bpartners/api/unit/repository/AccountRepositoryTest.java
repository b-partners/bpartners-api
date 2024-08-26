package app.bpartners.api.unit.repository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.rest.model.AccountStatus;
import app.bpartners.api.model.Account;
import app.bpartners.api.model.UpdateAccountIdentity;
import app.bpartners.api.repository.AccountRepository;
import app.bpartners.api.repository.implementation.AccountRepositoryImpl;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class AccountRepositoryTest {

  @Mock private AccountRepository accountRepository;

  @InjectMocks private AccountRepositoryImpl accountRepositoryImpl;

  private Account account;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);

    account =
        Account.builder()
            .id("1")
            .externalId("ext-1")
            .idAccountHolder("holder1")
            .userId("user1")
            .name("AccountName")
            .iban("IBAN123")
            .bic("BIC123")
            .active(true)
            .status(AccountStatus.OPENED)
            .build();
  }

  @Test
  void findByBearer_returnsAccountList() {
    when(accountRepository.findByBearer("someBearerToken")).thenReturn(List.of(account));

    List<Account> accounts = accountRepository.findByBearer("someBearerToken");
    assertNotNull(accounts);
    assertEquals(1, accounts.size());
    assertEquals(account, accounts.get(0));
  }

  @Test
  void findById_returnsAccount() {
    when(accountRepository.findById("1")).thenReturn(account);

    Account foundAccount = accountRepository.findById("1");
    assertNotNull(foundAccount);
    assertEquals(account, foundAccount);
  }

  @Test
  void findByUserId_returnsAccountList() {
    when(accountRepository.findByUserId("user1")).thenReturn(List.of(account));

    List<Account> accounts = accountRepository.findByUserId("user1");
    assertNotNull(accounts);
    assertEquals(1, accounts.size());
    assertEquals(account, accounts.get(0));
  }

  @Test
  void saveUpdateAccountIdentity_returnsUpdatedAccount() {
    UpdateAccountIdentity updateAccountIdentity = new UpdateAccountIdentity();
    Account updatedAccount = account.toBuilder().name("newName").iban("newIBAN").build();

    when(accountRepository.save(updateAccountIdentity)).thenReturn(updatedAccount);

    Account savedAccount = accountRepository.save(updateAccountIdentity);
    assertNotNull(savedAccount);
    assertEquals(updatedAccount, savedAccount);
  }

  @Test
  void saveAccount_returnsSavedAccount() {
    when(accountRepository.save(account)).thenReturn(account);

    Account savedAccount = accountRepository.save(account);
    assertNotNull(savedAccount);
    assertEquals(account, savedAccount);
  }

  @Test
  void saveAll_returnsSavedAccounts() {
    List<Account> accountsToSave = List.of(account);
    when(accountRepository.saveAll(accountsToSave)).thenReturn(accountsToSave);

    List<Account> savedAccounts = accountRepository.saveAll(accountsToSave);
    assertNotNull(savedAccounts);
    assertEquals(1, savedAccounts.size());
    assertEquals(account, savedAccounts.get(0));
  }

  @Test
  void removeAll_removesAccounts() {
    List<Account> accountsToRemove = List.of(account);
    doNothing().when(accountRepository).removeAll(accountsToRemove);

    accountRepository.removeAll(accountsToRemove);

    verify(accountRepository).removeAll(accountsToRemove);
  }

  @Test
  void findAll_returnsAllAccounts() {
    List<Account> accounts = List.of(account);
    when(accountRepository.findAll()).thenReturn(accounts);

    List<Account> allAccounts = accountRepository.findAll();
    assertNotNull(allAccounts);
    assertEquals(1, allAccounts.size());
    assertEquals(account, allAccounts.get(0));
  }
}
