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
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

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
    if (accountConnectors.isEmpty()) {
      return getJpaAccounts(AuthProvider.getAuthenticatedUserId());
    }
    return accountConnectors.stream()
        .map(this::convertConnector)
        .collect(Collectors.toList());
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
      return mapper.toDomain(entity, bankRepository.findById(entity.getIdBank()));
    }
    return convertConnector(accountConnector);
  }

  @Override
  public List<Account> findByUserId(String userId) {
    List<AccountConnector> accountConnectors = connectorRepository.findByUserId(userId);
    if (accountConnectors.isEmpty()) {
      return getJpaAccounts(userId);
    }
    return accountConnectors.stream()
        .map(this::convertConnector)
        .collect(Collectors.toList());
  }

  @Override
  public Account save(UpdateAccountIdentity updateAccount) {
    HAccount existing = jpaRepository.findById(updateAccount.getAccountId())
        .orElseThrow(
            () -> new NotFoundException(
                "Account(id=" + updateAccount.getAccountId() + ") not found"));
    HAccount saved = jpaRepository.save(existing.toBuilder()
        .name(updateAccount.getName() == null ? existing.getName() : updateAccount.getName())
        .iban(updateAccount.getIban() == null ? existing.getIban() : updateAccount.getIban())
        .bic(updateAccount.getBic())
        .build());
    return mapper.toDomain(saved,
        saved.getIdBank() == null ? null : bankRepository.findById(saved.getIdBank()));
  }

  @Override
  public Account save(Account toSave) {
    HUser user = userJpaRepository.findById(toSave.getUserId())
        .orElseThrow(() -> new NotFoundException(
            "User(id=" + toSave.getUserId() + " not found"));
    HAccount entity = mapper.toEntity(toSave, user);
    HAccount saved = jpaRepository.save(entity);
    return mapper.toDomain(saved,
        saved.getIdBank() == null ? null : bankRepository.findById(saved.getIdBank()));
  }

  private Account convertConnector(AccountConnector accountConnector) {
    String accountConnectorId = accountConnector.getId();
    HAccount entity = jpaRepository.findByExternalId(accountConnectorId)
        .orElseThrow(() -> new NotFoundException(
            "Account(externalId=" + accountConnectorId + ") not found"));
    return mapper.toDomain(accountConnector, entity,
        bankRepository.findByExternalId(accountConnector.getBankId()));
  }

  private List<Account> getJpaAccounts(String userId) {
    return jpaRepository.findByUser_Id(userId).stream()
        .map(entity -> mapper.toDomain(entity, bankRepository.findById(entity.getIdBank())))
        .collect(Collectors.toList());
  }
}
