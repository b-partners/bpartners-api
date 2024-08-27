package app.bpartners.api.unit.repository;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.rest.model.VerificationStatus;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.repository.AccountHolderRepository;
import app.bpartners.api.repository.implementation.AccountHolderRepositoryImpl;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;

class AccountHolderRepositoryTest {

  AccountHolderRepository accountHolderRepository;
  AccountHolderRepositoryImpl accountHolderRepositoryImpl;
  Pageable pageable;

  @BeforeEach
  void setUp() {
    accountHolderRepository = mock(AccountHolderRepository.class);
    accountHolderRepositoryImpl = mock(AccountHolderRepositoryImpl.class);
    pageable = mock(Pageable.class);

    pageable = Pageable.unpaged();
  }

  AccountHolder accountHolder(){
    return AccountHolder.builder()
            .id("1")
            .userId("user1")
            .name("Account Holder Name")
            .siren("123456789")
            .verificationStatus(VerificationStatus.VERIFIED)
            .build();
  }
  @Test
  void findAll_returnsAccountHolderList() {
    List<AccountHolder> accountHolders = List.of(accountHolder());

    when(accountHolderRepository.findAll(pageable)).thenReturn(accountHolders);

    List<AccountHolder> foundAccountHolders = accountHolderRepository.findAll(pageable);
    assertNotNull(foundAccountHolders);
    assertEquals(1, foundAccountHolders.size());
    assertEquals(accountHolder(), foundAccountHolders.get(0));
  }

  @Test
  void findAllByName_returnsAccountHolderList() {
    List<AccountHolder> accountHolders = List.of(accountHolder());

    when(accountHolderRepository.findAllByName("Account Holder Name", pageable))
        .thenReturn(accountHolders);

    List<AccountHolder> foundAccountHolders =
        accountHolderRepository.findAllByName("Account Holder Name", pageable);
    assertNotNull(foundAccountHolders);
    assertEquals(1, foundAccountHolders.size());
    assertEquals(accountHolder(), foundAccountHolders.get(0));
  }

  @Test
  void findAllByAccountId_returnsAccountHolderList() {
    List<AccountHolder> accountHolders = List.of(accountHolder());

    when(accountHolderRepository.findAllByAccountId("accountId")).thenReturn(accountHolders);

    List<AccountHolder> foundAccountHolders =
        accountHolderRepository.findAllByAccountId("accountId");
    assertNotNull(foundAccountHolders);
    assertEquals(1, foundAccountHolders.size());
    assertEquals(accountHolder(), foundAccountHolders.get(0));
  }

  @Test
  void findAllByUserId_returnsAccountHolderList() {
    List<AccountHolder> accountHolders = List.of(accountHolder());

    when(accountHolderRepository.findAllByUserId("user1")).thenReturn(accountHolders);

    List<AccountHolder> foundAccountHolders = accountHolderRepository.findAllByUserId("user1");
    assertNotNull(foundAccountHolders);
    assertEquals(1, foundAccountHolders.size());
    assertEquals(accountHolder(), foundAccountHolders.get(0));
  }

  @Test
  void save_returnsSavedAccountHolder() {
    when(accountHolderRepository.save(any())).thenReturn(accountHolder());

    AccountHolder savedAccountHolder = accountHolderRepository.save(accountHolder());
    assertNotNull(savedAccountHolder);
    assertEquals(accountHolder(), savedAccountHolder);
  }

  @Test
  void findById_returnsAccountHolder() {
    when(accountHolderRepository.findById("1")).thenReturn(accountHolder());

    AccountHolder foundAccountHolder = accountHolderRepository.findById("1");
    assertNotNull(foundAccountHolder);
    assertEquals(accountHolder(), foundAccountHolder);
  }

  @Test
  void findById_notFound() {
    when(accountHolderRepository.findById("999")).thenReturn(null);

    AccountHolder foundAccountHolder = accountHolderRepository.findById("999");
    assertNull(foundAccountHolder);
  }
}
