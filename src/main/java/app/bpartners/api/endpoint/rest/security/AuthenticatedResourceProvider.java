package app.bpartners.api.endpoint.rest.security;

import app.bpartners.api.model.Account;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.User;
import app.bpartners.api.service.account.AccountService;
import app.bpartners.api.service.accountholder.AccountHolderService;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AuthenticatedResourceProvider {
  private final AccountService accountService;
  private final AccountHolderService accountHolderService;

  public List<Account> getAccounts() {
    return accountService.getAccountsByBearer(AuthProvider.getBearer());
  }

  public Account getAccount() {
    return accountService.getActiveByBearer(AuthProvider.getBearer());
  }

  public AccountHolder getDefaultAccountHolder() {
    return accountHolderService.getDefaultByAccountId(getAccount().getId());
  }

  public List<AccountHolder> getAccountHolders() {
    return accountHolderService.getAccountHoldersByAccountId(getAccount().getId());
  }

  public User getUser() {
    return AuthProvider.getPrincipal().getUser();
  }
}
