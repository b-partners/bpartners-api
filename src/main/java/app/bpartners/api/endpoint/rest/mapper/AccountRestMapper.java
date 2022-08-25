package app.bpartners.api.endpoint.rest.mapper;


import app.bpartners.api.endpoint.rest.model.Account;
import org.springframework.stereotype.Component;

@Component
public class AccountRestMapper {
  public Account toRest(app.bpartners.api.model.Account internal) {
    Account restAccount = new Account();
    restAccount.setId(internal.getId());
    restAccount.setName(internal.getName());
    restAccount.setIBAN(internal.getIban());
    restAccount.setBIC(internal.getBic());
    return restAccount;
  }
}
