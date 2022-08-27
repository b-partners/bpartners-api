package app.bpartners.api.service;

import app.bpartners.api.model.Account;
import app.bpartners.api.repository.AccountRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AccountService {
  private AccountRepository repository;

  public List<Account> getAccounts() {
    return repository.findAll();
  }
}
