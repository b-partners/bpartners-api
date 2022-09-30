package app.bpartners.api.repository;

import app.bpartners.api.model.Account;
import java.util.List;

public interface AccountRepository {
  List<Account> findAll();

  List<Account> findByBearer(String bearer);

  Account findById(String id);

  List<Account> findByUserId(String userId);
}
