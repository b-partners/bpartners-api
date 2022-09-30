package app.bpartners.api.service;

import app.bpartners.api.model.Account;
import app.bpartners.api.model.validator.AccountValidator;
import app.bpartners.api.repository.AccountRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AccountService {
  private final AccountValidator validator;
  private final AccountRepository repository;

  public List<Account> getAccounts() {
    List<Account> accounts = repository.findAll();
    validator.accept(accounts);
    return accounts;
  }

  public Account getAccountByBearer(String bearer) {
    List<Account> accounts = repository.findByBearer(bearer);
    validator.accept(accounts);
    return accounts.get(0);
  }

  public Account getAccountById(String id) {
    return repository.findById(id);
  }

  public List<Account> getAccountsByUserId(String userId) {
    List<Account> accounts = repository.findByUserId(userId);
    validator.accept(accounts);
    return accounts;
  }
}
