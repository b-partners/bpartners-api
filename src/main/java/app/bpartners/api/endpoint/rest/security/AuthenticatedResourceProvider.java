package app.bpartners.api.endpoint.rest.security;

import app.bpartners.api.model.Account;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.service.AccountHolderService;
import app.bpartners.api.service.AccountService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AuthenticatedResourceProvider {
  private final AccountService accountService;
  private final AccountHolderService accountHolderService;

  public Account getAccount() {
    return accountService.getAccountByBearer(AuthProvider.getPrincipal().getBearer());
  }

  public AccountHolder getAccountHolder() {
    return accountHolderService.getAccountHolderByAccountId(
        getAccount().getId()
    );
  }
}
