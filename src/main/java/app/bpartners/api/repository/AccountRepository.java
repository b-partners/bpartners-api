package app.bpartners.api.repository;

import app.bpartners.api.model.Account;
import java.util.List;

public interface AccountRepository {
  List<Account> findAll();

  List<Account> findByBearer(String bearer);

  List<Account> findById(String userId);
}
