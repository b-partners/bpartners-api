package app.bpartners.api.repository;

import app.bpartners.api.model.Account;
import app.bpartners.api.model.UpdateAccountIdentity;
import java.util.List;

public interface AccountRepository {
  List<Account> findByBearer(String bearer);

  Account findById(String id);

  List<Account> findByUserId(String userId);

  Account save(UpdateAccountIdentity toSave);

  Account save(Account toSave);
}
