package app.bpartners.api.service;

import app.bpartners.api.endpoint.rest.model.BankConnectionRedirection;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.model.Account;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.UpdateAccountIdentity;
import app.bpartners.api.model.User;
import app.bpartners.api.model.UserToken;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.repository.AccountRepository;
import app.bpartners.api.repository.BankRepository;
import app.bpartners.api.repository.TransactionsSummaryRepository;
import app.bpartners.api.repository.UserRepository;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.api.service.utils.AccountUtils.describeAccountList;

@Service
@AllArgsConstructor
public class AccountService {
  private final AccountRepository repository;
  private final BankRepository bankRepository;
  private final UserRepository userRepository;
  private final TransactionsSummaryRepository summaryRepository;

  public Account getActive(List<Account> accounts) {
    return accounts.stream()
        .filter(Account::isActive)
        .findAny()
        .orElseThrow(() -> new NotImplementedException(
            "One account should be active but "
                + describeAccountList(accounts) + " do not contain active account"));
  }

  @Transactional
  public List<Account> findAllByActive(boolean status) {
    return repository.findAll().stream()
        .filter(account -> account.isActive() == status)
        .collect(Collectors.toList());
  }

  @Transactional
  public Account getActiveByBearer(String bearer) {
    return getActive(repository.findByBearer(bearer));
  }

  @Transactional
  public List<Account> getAccountsByBearer(String bearer) {
    return repository.findByBearer(bearer);
  }

  @Transactional
  public Account getAccountById(String id) {
    return repository.findById(id);
  }

  public Account updateAccountIdentity(UpdateAccountIdentity account) {
    return repository.save(account);
  }

  /*TODO: must not be order by active but consumers get(0) for now*/
  @Transactional
  public List<Account> getAccountsByUserId(String userId) {
    return repository.findByUserId(userId).stream()
        .sorted(Comparator.comparing(Account::isActive).reversed())
        .collect(Collectors.toList());
  }

  //TODO: IMPORTANT ! The obtained account here is the persisted account
  // Must get the most recent value from Bridge not from database
  // Need to update account from Bridge when getting account by ID
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

  public BankConnectionRedirection initiateBankConnection(
      String userId, RedirectionStatusUrls urls) {
    User user = userRepository.getById(userId);
    Account defaultAccount = user.getDefaultAccount();
    //TODO: map bank when mapping account inside userMapper and use it here
    if (user.getBankConnectionId() != null) {
      throw new BadRequestException(
          defaultAccount.describeMinInfos() + " is already connected to a bank."
              + " Disconnect before initiating another bank connection.");
    }
    String redirectionUrl = bankRepository.initiateConnection(user);
    resetDefaultAccount(userId, user, user.getDefaultAccount());
    return new BankConnectionRedirection()
        .redirectionUrl(redirectionUrl)
        .redirectionStatusUrls(urls);
  }

  //TODO: set into an event (bridge)
  @Transactional
  public Account disconnectBank(String userId) {
    User user = userRepository.getById(userId);
    List<Account> accounts = getAccountsByUserId(userId);
    Account active = getActive(accounts);

    if (bankRepository.disconnectBank(user)) {
      //Body of event bridge treatment
      summaryRepository.removeAll(userId);

      Account saved = repository.save(resetDefaultAccount(user, active));
      deleteOldAccounts(saved);

      userRepository.save(resetDefaultUser(user));
      //End of treatment
      return saved;
    }
    throw new ApiException(SERVER_EXCEPTION, active.describeInfos() + " was not disconnected");
  }

  private void deleteOldAccounts(Account saved) {
    List<Account> accounts = repository.findByUserId(saved.getUserId());
    accounts.remove(saved);
    repository.removeAll(accounts);
  }

  private Account resetDefaultAccount(User user, Account defaultAccount) {
    return defaultAccount.toBuilder()
        .name(user.getName())
        .bic(null)
        .iban(null)
        .bank(null)
        .availableBalance(new Fraction())
        .build();
  }

  private void resetDefaultAccount(String userId, User user, Account account) {
    Account defaultAccount = account.toBuilder()
        .userId(userId)
        .name(user.getName())
        .availableBalance(new Fraction())
        .bank(null)
        .bic(null)
        .iban(null)
        .build();
    repository.save(defaultAccount);
  }

  private User resetDefaultUser(User user) {
    return user.toBuilder()
        .preferredAccountId(null)
        .bankConnectionId(null)
        .bridgeItemUpdatedAt(Instant.now())
        .bridgeItemLastRefresh(Instant.now())
        .build();
  }

  @Transactional(isolation = Isolation.SERIALIZABLE)
  public Instant refreshBankConnection(UserToken userToken) {
    return bankRepository.refreshBankConnection(userToken);
  }
}
