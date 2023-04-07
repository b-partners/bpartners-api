package app.bpartners.api.service;

import app.bpartners.api.endpoint.rest.model.BankConnectionRedirection;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.model.Account;
import app.bpartners.api.model.User;
import app.bpartners.api.model.UserToken;
import app.bpartners.api.repository.AccountRepository;
import app.bpartners.api.repository.BankRepository;
import app.bpartners.api.repository.bridge.repository.BridgeBankRepository;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.transaction.annotation.Isolation.SERIALIZABLE;

@Service
@AllArgsConstructor
public class AccountService {
  private final AccountRepository repository;
  private final BridgeBankRepository bridgeBankRepository;
  private final BankRepository bankRepository;


  @Transactional(isolation = SERIALIZABLE)
  public Account getAccountByBearer(String bearer) {
    List<Account> accounts = repository.findByBearer(bearer);
    return accounts.get(0);
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

  //TODO: use bank repository and do not expose BridgeBankRepository
  public BankConnectionRedirection getBankConnectionInitUrl(
      String userId, String accountId, RedirectionStatusUrls urls) {
    User authenticatedUser = AuthProvider.getPrincipal().getUser();
    return new BankConnectionRedirection()
        .redirectionUrl(bridgeBankRepository.initiateBankConnection(authenticatedUser.getEmail()))
        .redirectionStatusUrls(urls);
  }

  @Transactional(isolation = Isolation.SERIALIZABLE)
  public Instant refreshBankConnection(UserToken userToken) {
    return bankRepository.refreshBankConnection(userToken);
  }
}
