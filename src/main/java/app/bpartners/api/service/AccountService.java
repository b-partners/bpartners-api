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

  public List<Account> findAccountsByUserId(String id) {
    List<Account> accounts = repository.findByUserId(id);
    validator.accept(accounts);
    return accounts;
  }
}
