package app.bpartners.api.repository.implementation;

import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.model.Account;
import app.bpartners.api.model.Bank;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.AccountMapper;
import app.bpartners.api.repository.AccountRepository;
import app.bpartners.api.repository.BankRepository;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Repository;
import org.springframework.web.servlet.view.RedirectView;

@Repository
@AllArgsConstructor
@Slf4j
public class AccountRepositoryImpl implements AccountRepository {
  private final AccountSwanRepository swanRepository;
  private final AccountJpaRepository accountJpaRepository;
  private final UserRepository userRepository;
  private final AccountMapper mapper;
  private final UserJpaRepository userJpaRepository;

  /* TODO: BAD! You should NOT have Swan and Bridge implemented in the same class!
   *   Instead, you should have a SwanAccountRepositoryImpl,
   *   and a BridgeAccountRepositoryImpl.
   *   As is, with the two implementations mixed in a same class,
   *   the AccountRepository interface is completely useless! */
  private final BridgeAccountRepository bridgeRepository;
  private final BankRepository bankRepository;

  //TODO: delete bridge item if persisted account has bank_id before mapping new values
  @Override
  public List<Account> findByBearer(String bearer) {
    User authenticatedUser = userRepository.getUserByToken(bearer);

    List<SwanAccount> swanAccounts = swanRepository.findByBearer(bearer);
    if (swanAccounts.isEmpty()) {
      List<BridgeAccount> bridgeAccounts = bridgeRepository.findByBearer(bearer);
      if (bridgeAccounts.isEmpty()) {
        //TODO: this if-statement is super dubious: it has nothing to do with the input bearer!!!
        return List.of(authenticatedUser.getAccount());
      }
      return bridgeAccounts.stream()
          .map(bridgeAccount -> getUpdatedAccount(authenticatedUser, bridgeAccount))
          .collect(Collectors.toList());
    }
    return getUpdatedAccounts(swanAccounts, authenticatedUser.getId());
  }

  @Override
  public Account findById(String accountId) {
    User authenticatedUser = userIsAuthenticated()
        ? AuthProvider.getPrincipal().getUser() : null;

    List<SwanAccount> swanAccounts = userIsAuthenticated()
        ? swanRepository.findById(accountId)
        : List.of();
    if (swanAccounts.isEmpty()) {
      HAccount account = accountJpaRepository.findById(accountId).orElseThrow(
          () -> new NotFoundException("Account." + accountId + " not found.")
      );
      return mapper.toDomain(account, account.getUser().getId());
    }
    return getUpdatedAccounts(
        swanAccounts, userIsAuthenticated() ? authenticatedUser.getId() : null).get(0);
  }

  @Override
  public List<Account> findByUserId(String userId) {
    User authenticatedUser = userIsAuthenticated()
        ? AuthProvider.getPrincipal().getUser() : null;

    List<SwanAccount> swanAccounts = swanRepository.findByUserId(userId);
    if (swanAccounts.isEmpty()) {
      bankRepository.selfUpdateBankConnection();
      List<BridgeAccount> bridgeAccounts = bridgeRepository.findAllByAuthenticatedUser();
      if (bridgeAccounts.isEmpty()) {
        return List.of(mapper.toDomain(accountJpaRepository.findByUser_Id(userId).orElseThrow(
                () -> new NotFoundException("User." + userId + " is not associated with any account")),
            userId
        ));
      }
      return bridgeAccounts.stream()
          .map(bridgeAccount -> getUpdatedAccount(authenticatedUser, bridgeAccount))
          .collect(Collectors.toList());
    }
    return getUpdatedAccounts(swanAccounts, userId);
  }

  @Override
  public List<Account> saveAll(List<Account> toCreate, String userId) {
    HUser user = userJpaRepository.findById(userId).orElseThrow(
        () -> new NotFoundException("User." + userId + " not found"));
    return saveAll(toCreate, user);
  }

  private List<Account> saveAll(List<Account> toCreate, HUser user) {
    List<HAccount> toSave = toCreate.stream()
        .map(account -> mapper.toEntity(account, user))
        .collect(Collectors.toList());
    return accountJpaRepository.saveAll(toSave).stream()
        .map(account -> mapper.toDomain(account, user.getId()))
        .collect(Collectors.toList());
  }

  @Override
  public Account save(Account domain, String userId) {
    HUser userEntity = userJpaRepository.findById(userId).orElseThrow(
        () -> new NotFoundException("User." + userId + " not found"));
    return save(domain, userEntity);
  }

  @Override
  public RedirectView validateConnection() {
    return bankRepository.validateProItems();
  }

  private Account save(Account domain, HUser user) {
    HAccount entityToSave = mapper.toEntity(domain, user);
    HAccount savedEntity = accountJpaRepository.save(entityToSave);
    return mapper.toDomain(
        savedEntity, bankRepository.findById(savedEntity.getIdBank()), user.getId());
  }

  private Account getUpdatedAccount(User authenticatedUser, BridgeAccount bridgeAccount) {
    Bank bank = bankRepository.findByBridgeId(bridgeAccount.getBankId());
    if (authenticatedUser == null) {
      return mapper.toDomain(bridgeAccount, bank, null, null);
    } else {
      String userId = authenticatedUser.getId();
      return save(
          mapper.toDomain(
              bridgeAccount, bank, authenticatedUser.getAccount(), userId), userId);
    }
  }

  //TODO: simplify the cognitive complexity
  private List<Account> getUpdatedAccounts(List<SwanAccount> swanAccounts, String userId) {
    SwanAccount swanAccount = swanAccounts.get(0);
    Optional<HAccount> persisted = accountJpaRepository.findById(swanAccount.getId());
    if (persisted.isPresent()) {
      String actualUserId = persisted.get().getUser() == null
          ? userId
          : persisted.get().getUser().getId();
      List<Account> accounts =
          List.of(mapper.toDomain(swanAccount, persisted.get(), actualUserId));
      if (userId == null || actualUserId == null) {
        return accounts;
      }
      return saveAll(accounts, actualUserId);
    } else {
      List<Account> accounts = List.of(mapper.toDomain(swanAccount, userId));
      if (userId == null) {
        return accounts;
      }
      return saveAll(accounts, userId);
    }
  }

  private boolean userIsAuthenticated() {
    return SecurityContextHolder.getContext().getAuthentication() != null;
  }
}
