package app.bpartners.api.repository;

import app.bpartners.api.model.Account;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountRepository {
  List<Account> getAccounts();
}
