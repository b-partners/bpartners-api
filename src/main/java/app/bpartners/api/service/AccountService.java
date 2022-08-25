package app.bpartners.api.service;

import app.bpartners.api.model.Account;
import app.bpartners.api.repository.AccountRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class AccountService {
  private AccountRepository repository;

  public List<Account> getAccounts(){
    return repository.getAccounts();
  };
}
