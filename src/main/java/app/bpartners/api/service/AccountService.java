package app.bpartners.api.service;

import app.bpartners.api.endpoint.rest.security.model.Principal;
import app.bpartners.api.endpoint.rest.security.principal.PrincipalProvider;
import app.bpartners.api.model.Account;
import app.bpartners.api.model.exception.ForbiddenException;
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
  private final PrincipalProvider principalProvider;

  public List<Account> getAccounts() {
    List<Account> accounts = repository.findAll();
    validator.accept(accounts);
    return accounts;
  }

  private Principal getPrincipal() {
    return (Principal) principalProvider.getAuthentication().getPrincipal();
  }

  public List<Account> getAccountsByUserId(String userId) {
    // TODO: Verify if the user exists or not
    if (getPrincipal().getUserId().equals(userId)) {
      List<Account> accounts = repository.findAll();
      validator.accept(accounts);
      return accounts;
    } else {
      throw new ForbiddenException();
    }
  }
}
