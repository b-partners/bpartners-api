package app.bpartners.api.service;

import app.bpartners.api.model.Account;
import app.bpartners.api.repository.AccountRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AccountService {
  private final AccountRepository repository;

  public Account getAccountByBearer(String bearer) {
    List<Account> accounts = repository.findByBearer(bearer);
    return accounts.get(0);
  }

  public Account getAccountById(String id) {
    return repository.findById(id);
  }

  public List<Account> getAccountsByUserId(String userId) {
    return repository.findByUserId(userId);
  }
}
