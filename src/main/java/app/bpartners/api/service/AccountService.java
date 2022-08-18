package app.bpartners.api.service;

import app.bpartners.api.model.Account;
import app.bpartners.api.repository.AccountRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AccountService {
  private final AccountRepository accountRepository;

  public Account getAccount() {
    return accountRepository.getAccount();
  }
}
