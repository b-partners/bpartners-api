package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.Account;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.AccountMapper;
import app.bpartners.api.repository.AccountRepository;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.bridge.model.Account.BridgeAccount;
import app.bpartners.api.repository.bridge.repository.BridgeAccountRepository;
import app.bpartners.api.repository.jpa.AccountJpaRepository;
import app.bpartners.api.repository.jpa.UserJpaRepository;
import app.bpartners.api.repository.jpa.model.HAccount;
import app.bpartners.api.repository.jpa.model.HUser;
import app.bpartners.api.repository.swan.AccountSwanRepository;
import app.bpartners.api.repository.swan.model.SwanAccount;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
@Slf4j
public class AccountRepositoryImpl implements AccountRepository {
  private final AccountSwanRepository swanRepository;
  private final AccountJpaRepository accountJpaRepository;
  private final UserRepository userRepository;
  private final AccountMapper mapper;
  private final UserJpaRepository userJpaRepository;
  private final BridgeAccountRepository bridgeRepository;

  @Override
  public List<Account> findByBearer(String bearer) {
    List<SwanAccount> swanAccounts = swanRepository.findByBearer(bearer);
    User authenticatedUser = userRepository.getUserByToken(bearer);
    if (!swanAccounts.isEmpty()) {
      return getOrCreateAccounts(swanAccounts, authenticatedUser.getId());
    }
    List<BridgeAccount> bridgeAccounts = bridgeRepository.findByBearer(bearer);
    if (!bridgeAccounts.isEmpty()) {
      //TODO: getOrUpdate accounts
      return bridgeAccounts.stream()
          .map(bridgeAccount -> mapper.toDomain(bridgeAccount, authenticatedUser.getId()))
          .collect(Collectors.toList());
    }
    return List.of(authenticatedUser.getAccount());
  }

  @Override
  public Account findById(String accountId) {
    List<SwanAccount> swanAccounts = swanRepository.findById(accountId);
    if (!swanAccounts.isEmpty()) {
      return getOrCreateAccounts(swanAccounts, null).get(0);
    }
    BridgeAccount bridgeAccount = bridgeRepository.findById(accountId);
    if (bridgeAccount != null) {
      //TODO: getOrUpdate accounts
      return mapper.toDomain(bridgeAccount, null);
    }
    Optional<HAccount> optionalAccount = accountJpaRepository.findById(accountId);
    if (optionalAccount.isPresent()) {
      return mapper.toDomain(optionalAccount.get(), optionalAccount.get().getUser().getId());
    } else {
      throw new NotFoundException("Account." + accountId + " not found.");
    }
  }

  @Override
  public List<Account> findByUserId(String userId) {
    List<SwanAccount> swanAccounts = swanRepository.findByUserId(userId);
    if (!swanAccounts.isEmpty()) {
      return getOrCreateAccounts(swanAccounts, userId);
    }
    List<BridgeAccount> bridgeAccounts = bridgeRepository.findAllByAuthenticatedUser();
    if (!bridgeAccounts.isEmpty()) {
      //TODO: getOrUpdate accounts
      return bridgeAccounts.stream()
          .map(bridgeAccount -> mapper.toDomain(bridgeAccount, userId))
          .collect(Collectors.toList());
    }
    Optional<HAccount> optionalAccount = accountJpaRepository.findByUser_Id(userId);
    if (optionalAccount.isPresent()) {
      return List.of(mapper.toDomain(optionalAccount.get(), userId));
    } else {
      throw new NotFoundException("User." + userId + " is not associated with any account");
    }
  }

  @Override
  public List<Account> saveAll(List<Account> toCreate, String userId) {
    HUser user = userJpaRepository.findById(userId).orElseThrow(
        () -> new NotFoundException("User." + userId + " not found")
    );
    return saveAll(toCreate, user);
  }

  public List<Account> saveAll(List<Account> toCreate, HUser user) {
    List<HAccount> toSave = toCreate.stream()
        .map(account -> mapper.toEntity(account, user))
        .collect(Collectors.toList());
    return accountJpaRepository.saveAll(toSave).stream()
        .map(account -> mapper.toDomain(account, user.getId()))
        .collect(Collectors.toList());
  }

  //TODO: simplify the cognitive complexity
  private List<Account> getOrCreateAccounts(List<SwanAccount> swanAccounts, String userId) {
    SwanAccount swanAccount = swanAccounts.get(0);
    Optional<HAccount> persisted = accountJpaRepository.findById(swanAccount.getId());
    if (persisted.isPresent()) {
      String persistedUserId = persisted.get().getUser() == null ? null
          : persisted.get().getUser().getId();
      List<Account> accounts =
          List.of(mapper.toDomain(swanAccount, persisted.get(), persistedUserId));
      if (userId == null || persistedUserId == null) {
        return accounts;
      }
      return saveAll(accounts, persistedUserId);
    } else {
      List<Account> accounts = List.of(mapper.toDomain(swanAccount, userId));
      if (userId == null) {
        return accounts;
      }
      return saveAll(accounts, userId);
    }
  }
}
