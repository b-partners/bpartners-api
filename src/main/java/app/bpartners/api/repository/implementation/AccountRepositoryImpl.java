package app.bpartners.api.repository.implementation;

import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.model.Account;
import app.bpartners.api.model.UpdateAccountIdentity;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.AccountMapper;
import app.bpartners.api.repository.AccountConnectorRepository;
import app.bpartners.api.repository.AccountRepository;
import app.bpartners.api.repository.BankRepository;
import app.bpartners.api.repository.jpa.AccountJpaRepository;
import app.bpartners.api.repository.jpa.UserJpaRepository;
import app.bpartners.api.repository.jpa.model.HAccount;
import app.bpartners.api.repository.jpa.model.HUser;
import app.bpartners.api.repository.model.AccountConnector;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import static app.bpartners.api.service.utils.AccountUtils.filterActive;
import static app.bpartners.api.service.utils.FilterUtils.distinctByKeys;

@Repository
@AllArgsConstructor
@Slf4j
public class AccountRepositoryImpl implements AccountRepository {
  private final AccountMapper mapper;
  private final AccountConnectorRepository connectorRepository;
  private final AccountJpaRepository jpaRepository;
  private final UserJpaRepository userJpaRepository;
  private final BankRepository bankRepository;

  /*
    TODO:
    /!\ IMPORTANT NOTE !
    To respect Facade pattern abstraction, JPA Repository must be a connector as Swan or Bridge.
    So the entities must be return through AccountConnector first, then convert into Account
    through JPA inside this class AccountRepositoryImpl.
    Meanwhile, to avoid two database requests, we broke the abstraction here.
     */
  @Override
  public List<Account> findByBearer(String bearer) {
    List<AccountConnector> accountConnectors = connectorRepository.findByBearer(bearer);
    String preferredAccountId = AuthProvider.getPreferredAccountId();
    List<Account> jpaAccounts =
        getJpaAccounts(AuthProvider.getAuthenticatedUserId(), preferredAccountId);
    return combineAccounts(preferredAccountId, accountConnectors, jpaAccounts);
  }

  private List<Account> convertConnectors(
      String preferredAccountId, List<AccountConnector> accountConnectors) {
    return filterByActive(preferredAccountId, accountConnectors.stream()
        .map(this::convertConnector)
        .collect(Collectors.toList()));
  }

  @Override
  public Account findById(String id) {
    HAccount entity = jpaRepository.findById(id).orElseThrow(
        () -> new NotFoundException("Account(id=" + id + ") not found"));
    String externalId = entity.getExternalId() == null
        ? entity.getId() //case when external ID equals ID (Default for Swan)
        : entity.getExternalId(); // Default for Bridge
    AccountConnector accountConnector = connectorRepository.findById(externalId);
    if (accountConnector == null) {
      return mapper.toDomain(entity, bankRepository.findByExternalId(entity.getIdBank()));
    }
    return mapper.toDomain(accountConnector, entity,
        bankRepository.findByExternalId(accountConnector.getBankId()));
  }

  @Override
  public List<Account> findByUserId(String userId) {
    HUser user = getUserById(userId);
    String preferredAccountId = user.getPreferredAccountId();
    List<AccountConnector> accountConnectors = connectorRepository.findByUserId(userId);
    List<Account> jpaAccounts = getJpaAccounts(userId, preferredAccountId);
    return combineAccounts(preferredAccountId, accountConnectors, jpaAccounts);
  }

  @Override
  public Account save(UpdateAccountIdentity updateAccount) {
    Account account = findById(updateAccount.getAccountId());
    HUser user = getUserById(account.getUserId());
    HAccount existing = mapper.toEntity(account, user);
    HAccount saved = jpaRepository.save(existing.toBuilder()
        .name(updateAccount.getName() == null ? existing.getName() : updateAccount.getName())
        .iban(updateAccount.getIban() == null ? existing.getIban() : updateAccount.getIban())
        .bic(updateAccount.getBic())
        .build());
    return mapper.toDomain(saved,
        saved.getIdBank() == null ? null : bankRepository.findByExternalId(saved.getIdBank()));
  }

  @Override
  public Account save(Account toSave) {
    HUser user = getUserById(toSave.getUserId());
    HAccount entity = mapper.toEntity(toSave, user);
    HAccount saved = jpaRepository.save(entity);
    return mapper.toDomain(saved,
        saved.getIdBank() == null ? null : bankRepository.findByExternalId(saved.getIdBank()));
  }

  @Override
  public void removeAll(List<Account> toRemove) {
    List<String> ids = new ArrayList<>();
    toRemove.forEach(
        account -> ids.add(account.getId())
    );
    jpaRepository.deleteAllById(ids);
  }

  @Override
  public List<Account> findAll() {
    return jpaRepository.findAll().stream()
        .map(account -> mapper.toDomain(account,
            bankRepository.findByExternalId(account.getIdBank())))
        .collect(Collectors.toList());
  }

  private Account convertConnector(AccountConnector accountConnector) {
    String accountConnectorId = accountConnector.getId();
    HAccount entity = jpaRepository.findByExternalId(accountConnectorId)
        .orElseThrow(() -> new NotFoundException(
            "Account(externalId=" + accountConnectorId + ") not found"));
    return mapper.toDomain(accountConnector, entity,
        bankRepository.findByExternalId(accountConnector.getBankId()));
  }

  private List<Account> getJpaAccounts(String userId, String preferredAccountId) {
    return filterByActive(preferredAccountId, jpaRepository.findByUser_Id(userId).stream()
        .map(entity -> mapper.toDomain(entity, bankRepository.findByExternalId(entity.getIdBank())))
        .collect(Collectors.toList()));
  }

  private List<Account> filterByActive(String preferredAccountId, List<Account> accounts) {
    Account activeAccount = filterActive(accounts, preferredAccountId);
    if (!activeAccount.isActive()) {
      activeAccount.active(true);
    }
    return accounts;
  }

  private List<Account> combineAccounts(
      String preferredAccountId,
      List<AccountConnector> accountConnectors,
      List<Account> jpaAccounts) {
    if (accountConnectors.isEmpty()) {
      return jpaAccounts;
    }
    List<Account> convertedAccounts = convertConnectors(preferredAccountId, accountConnectors);
    convertedAccounts.addAll(jpaAccounts);
    return convertedAccounts.stream()
        .filter(distinctByKeys(Account::getId))
        .collect(Collectors.toList());
  }

  private HUser getUserById(String idUser) {
    return userJpaRepository.findById(idUser)
        .orElseThrow(() -> new NotFoundException(
            "User(id=" + idUser + " not found"));
  }
}
