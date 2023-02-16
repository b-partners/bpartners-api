package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.Account;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.AccountMapper;
import app.bpartners.api.repository.AccountRepository;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.jpa.AccountJpaRepository;
import app.bpartners.api.repository.jpa.model.HAccount;
import app.bpartners.api.repository.swan.AccountSwanRepository;
import app.bpartners.api.repository.swan.model.SwanAccount;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class AccountRepositoryImpl implements AccountRepository {
  private static final String JOE_DOE_SWAN_USER_ID = "c15924bf-61f9-4381-8c9b-d34369bf91f7";
  private AccountSwanRepository swanRepository;
  private AccountJpaRepository accountJpaRepository;
  private UserRepository userRepository;
  private AccountMapper mapper;

  @Override
  public List<Account> findByBearer(String bearer) {
    List<SwanAccount> swanAccounts = swanRepository.findByBearer(bearer);
    List<Account> persisted = getOrCreateAccounts(swanAccounts);
    if (persisted.isEmpty()) {
      User authenticatedUser = userRepository.getUserByToken(bearer);
      if (authenticatedUser == null) {
        return findByUserId(JOE_DOE_SWAN_USER_ID);
      } else {
        return findByUserId(authenticatedUser.getId());
      }
    }
    return persisted;
  }

  @Override
  public Account findById(String accountId) {
    List<SwanAccount> accounts = swanRepository.findById(accountId);
    List<Account> persistedAccounts = getOrCreateAccounts(accounts);
    if (persistedAccounts.isEmpty()) {
      Optional<HAccount> optionalAccount = accountJpaRepository.findById(accountId);
      if (optionalAccount.isPresent()) {
        return mapper.toDomain(optionalAccount.get());
      } else {
        throw new NotFoundException("Account." + accountId + " not found.");
      }
    }
    return persistedAccounts.get(0);
  }

  @Override
  public List<Account> findByUserId(String userId) {
    List<SwanAccount> accounts = swanRepository.findByUserId(userId);
    return getOrCreateAccounts(accounts);
  }

  @Override
  public List<Account> saveAll(List<Account> toCreate) {
    List<HAccount> toSave = toCreate.stream()
        .map(mapper::toEntity)
        .collect(Collectors.toUnmodifiableList());
    return accountJpaRepository.saveAll(toSave).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
  }

  private List<Account> getOrCreateAccounts(List<SwanAccount> swanAccounts) {
    if (!swanAccounts.isEmpty()) {
      Optional<HAccount> persisted = accountJpaRepository.findById(swanAccounts.get(0).getId());
      if (persisted.isPresent()) {
        return saveAll(List.of(mapper.toDomain(swanAccounts.get(0), persisted.get())));
      } else {
        List<Account> accounts = swanAccounts.stream()
            .map(mapper::toDomain)
            .collect(Collectors.toUnmodifiableList());
        return saveAll(accounts);
      }
    }
    return List.of();
  }
}
