package app.bpartners.api.service;

import app.bpartners.api.endpoint.rest.model.BankConnectionRedirection;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.model.Account;
import app.bpartners.api.model.User;
import app.bpartners.api.model.UserToken;
import app.bpartners.api.repository.AccountRepository;
import app.bpartners.api.repository.BankRepository;
import app.bpartners.api.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

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
    return new BankConnectionRedirection()
        .redirectionUrl(bankRepository.initiateConnection(user))
        .redirectionStatusUrls(urls);
  }

  @Transactional(isolation = Isolation.SERIALIZABLE)
  public Instant refreshBankConnection(UserToken userToken) {
    return bankRepository.refreshBankConnection(userToken);
  }
}
