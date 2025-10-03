package app.bpartners.api.service.account;

import static app.bpartners.api.endpoint.rest.model.AccountStatus.OPENED;
import static app.bpartners.api.service.utils.AccountUtils.describeAccountList;
import static java.util.UUID.randomUUID;

import app.bpartners.api.endpoint.rest.model.EnableStatus;
import app.bpartners.api.model.Account;
import app.bpartners.api.model.Money;
import app.bpartners.api.model.UpdateAccountIdentity;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.repository.AccountRepository;
import app.bpartners.api.repository.UserRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class AccountService {
  private final AccountRepository repository;
  private final UserRepository userRepository;

  public Account getActive(List<Account> accounts) {
    return accounts.stream()
        .filter(account -> account.isActive() && account.isEnabled())
        .findAny()
        .orElseThrow(
            () ->
                new NotImplementedException(
                    "One account should be active but "
                        + describeAccountList(accounts)
                        + " do not contain active account"));
  }

  @Transactional
  public Account getActiveByBearer(String bearer) {
    return getActive(repository.findByBearer(bearer));
  }

  @Transactional
  public List<Account> getAccountsByBearer(String bearer) {
    return repository.findByBearer(bearer).stream()
        .filter(app.bpartners.api.model.Account::isEnabled)
        .toList();
  }

  @Transactional
  public Account save(Account toSave) {
    return repository.save(toSave);
  }

  @Transactional
  public Account updateAccountIdentity(UpdateAccountIdentity account) {
    return repository.save(account);
  }

  /*TODO: must not be order by active but consumers get(0) for now*/
  @Transactional
  public List<Account> getAccountsByUserId(String userId) {
    return repository.findByUserId(userId).stream()
        .filter(app.bpartners.api.model.Account::isEnabled)
        .sorted(Comparator.comparing(Account::isActive).reversed())
        .collect(Collectors.toList());
  }

  @Transactional
  public List<Account> findAllActiveAccounts() {
    List<User> users = userRepository.findAll();
    List<Account> activeAccounts = new ArrayList<>();
    users.forEach(
        user -> {
          if (user.getDefaultAccount() != null) {
            activeAccounts.add(user.getDefaultAccount());
          }
        });
    return activeAccounts;
  }

  public static Account resetDefaultAccount(User user, Account defaultAccount) {
    return defaultAccount.toBuilder()
        .id(String.valueOf(randomUUID()))
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
  }

  public static User resetDefaultUser(User user, Account account) {
    return user.toBuilder()
        .preferredAccountId(account.getId())
        .bankConnectionId(null)
        .connectionStatus(null)
        .bridgeItemLastRefresh(null)
        .bridgeItemUpdatedAt(Instant.now())
        .build();
  }

  public List<Account> saveAll(List<Account> accounts) {
    return repository.saveAll(accounts);
  }
}
