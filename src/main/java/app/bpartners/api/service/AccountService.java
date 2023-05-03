package app.bpartners.api.service;

import app.bpartners.api.endpoint.rest.model.BankConnectionRedirection;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.model.Account;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.User;
import app.bpartners.api.model.UserToken;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.repository.AccountRepository;
import app.bpartners.api.repository.BankRepository;
import app.bpartners.api.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

@Service
@AllArgsConstructor
public class AccountService {
  private final AccountRepository repository;
  private final BankRepository bankRepository;
  private final UserRepository userRepository;


  @Transactional
  public Account getAccountByBearer(String bearer) {
    List<Account> accounts = repository.findByBearer(bearer);
    return accounts.get(0); //TODO: do NOT get(0) like this without warning
  }

  public Account getAccountById(String id) {
    return repository.findById(id);
  }

  public Account updateAccountIdentity(String userId, Account account) {
    return repository.save(account, userId);
  }

  public List<Account> getAccountsByUserId(String userId) {
    return repository.findByUserId(userId);
  }

  public BankConnectionRedirection getBankConnectionInitUrl(
      String userId, RedirectionStatusUrls urls) {
    User user = userRepository.getById(userId);
    String redirectionUrl = bankRepository.initiateConnection(user);
    resetDefaultAccount(userId, user, user.getAccount());
    return new BankConnectionRedirection()
        .redirectionUrl(redirectionUrl)
        .redirectionStatusUrls(urls);
  }

  public Account disconnectBank(String userId) {
    User user = userRepository.getById(userId);
    Account account = user.getAccount(); /*TODO: Should get the actual account status*/
    if (account.getIban() == null && account.getBank() == null) {
      throw new BadRequestException("Only account associated to a bank can be disconnected, but "
          + "Account(id=" + account.getId() + ",name=" + account.getName()
          + ") does not have bank and iban");
    }

    if (bankRepository.disconnectBank(user)) {
      /*TODO: get default account without bank*/
      Account defaultAccount = repository.findByUserId(user.getId()).get(0);
      Account saved = repository.save(defaultAccount.toBuilder()
          .bic(null)
          .iban(null)
          .build(), userId);
      return saved;
    }
    throw new ApiException(SERVER_EXCEPTION,
        "Account(id=" + account.getId() + ",name=" + account.getName() + "iban="
            + account.getIban() + ") was not disconnected");
  }

  private void resetDefaultAccount(String userId, User user, Account account) {
    Account defaultAccount = account.toBuilder()
        .name(user.getName())
        .bridgeAccountId(null)
        .availableBalance(new Fraction())
        .bank(null)
        .bic(null)
        .iban(null)
        .build();
    repository.save(defaultAccount, userId);
  }

  @Transactional(isolation = Isolation.SERIALIZABLE)
  public Instant refreshBankConnection(UserToken userToken) {
    return bankRepository.refreshBankConnection(userToken);
  }
}
