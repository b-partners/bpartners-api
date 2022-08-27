package app.bpartners.api.repository;

import app.bpartners.api.model.Account;
import java.util.List;
import org.springframework.data.repository.CrudRepository;

public interface AccountRepository {
  List<Account> findAll(); //TODO(use standard repository function names): getAccounts --> findAll
}
