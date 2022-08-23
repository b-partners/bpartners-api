package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.Account;
import org.springframework.stereotype.Component;

@Component
public class AccountRestMapper {
  public Account toRest(app.bpartners.api.model.Account internal) {
    Account account = new Account();
    account.setAccountId(internal.getId());
    account.setNumber(internal.getNumber());
    account.setAccountName(internal.getName());
    account.setBIC(internal.getBIC());
    account.setIBAN(internal.getIBAN());
    return account;
  }
}
