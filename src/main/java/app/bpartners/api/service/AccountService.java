package app.bpartners.api.service;

import static app.bpartners.api.endpoint.rest.model.AccountStatus.OPENED;
import static app.bpartners.api.repository.implementation.BankRepositoryImpl.TRY_AGAIN;
import static app.bpartners.api.service.utils.AccountUtils.describeAccountList;
import static java.util.UUID.randomUUID;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.DisconnectionInitiated;
import app.bpartners.api.endpoint.rest.model.BankConnectionRedirection;
import app.bpartners.api.endpoint.rest.model.EnableStatus;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.model.Account;
import app.bpartners.api.model.Money;
import app.bpartners.api.model.UpdateAccountIdentity;
import app.bpartners.api.model.User;
import app.bpartners.api.model.UserToken;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.repository.AccountRepository;
import app.bpartners.api.repository.BankRepository;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.repository.bridge.BridgeApi;
import app.bpartners.api.repository.bridge.model.Item.BridgeItem;
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
  private final BankRepository bankRepository;
  private final UserRepository userRepository;
  private final BridgeApi bridgeApi;
  private final EventProducer<DisconnectionInitiated> eventProducer;

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
  public Account getAccountById(String id) {
    return repository.findById(id);
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

  // TODO: IMPORTANT ! The obtained account here is the persisted account
  // Must get the most recent value from Bridge not from database
  // Need to update account from Bridge when getting account by ID
  @Transactional
  public String initiateAccountValidation(String accountId) {
    Account account = repository.findById(accountId);
    switch (account.getStatus()) {
      case VALIDATION_REQUIRED:
        return bankRepository.initiateProValidation(accountId);
      case INVALID_CREDENTIALS:
        return bankRepository.initiateBankConnectionEdition(account);
      case SCA_REQUIRED:
        return bankRepository.initiateScaSync(account);
      default:
        throw new BadRequestException(account.describeInfos() + " does not need validation.");
    }
  }

  @Transactional
  public BankConnectionRedirection initiateBankConnection(
      String userId, RedirectionStatusUrls urls) {
    User user = userRepository.getById(userId);
    Account defaultAccount =
        user.getAccounts().stream()
            .filter(
                account ->
                    account.getExternalId() == null && account.getName().contains(user.getName()))
            .findAny()
            .orElse(user.getDefaultAccount());
    // TODO: map bank when mapping account inside userMapper and use it here
    if (user.getBankConnectionId() != null && user.getBankConnectionId() != TRY_AGAIN) {
      throw new BadRequestException(
          defaultAccount.describeMinInfos()
              + " is already connected to a bank."
              + " Disconnect before initiating another bank connection.");
    }
    String redirectionUrl = bankRepository.initiateConnection(user);
    resetDefaultAccount(userId, user, defaultAccount);
    return new BankConnectionRedirection()
        .redirectionUrl(redirectionUrl)
        .redirectionStatusUrls(urls);
  }

  @Transactional
  public Account disconnectBank(String userId) {
    User user = userRepository.getById(userId);
    List<BridgeItem> bridgeBankConnections = bridgeApi.findItemsByToken(user.getAccessToken());
    if (bridgeBankConnections.isEmpty()) {
      throw new BadRequestException(
          "User(id="
              + userId
              + ",name="
              + user.getName()
              + ")"
              + " is not still connected to a bank");
    }
    if (bankRepository.disconnectBank(user)) {
      eventProducer.accept(List.of(new DisconnectionInitiated(userId)));
    }
    return user.getDefaultAccount();
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

  private void resetDefaultAccount(String userId, User user, Account account) {
    Account defaultAccount =
        account.toBuilder().userId(userId).bank(null).bic(null).iban(null).externalId(null).build();
    repository.save(defaultAccount);
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

  @Transactional
  public Instant refreshBankConnection(UserToken userToken) {
    return bankRepository.refreshBankConnection(userToken);
  }

  public List<Account> saveAll(List<Account> accounts) {
    return repository.saveAll(accounts);
  }
}
