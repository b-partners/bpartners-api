package app.bpartners.api.model.validator;

import app.bpartners.api.model.Account;
import app.bpartners.api.model.exception.NotImplementedException;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class AccountValidator {
  public void accept(List<Account> accounts) {
    if (accounts.size() > 2) {
      throw new NotImplementedException("One account for one user is supported");
    }
  }
}
