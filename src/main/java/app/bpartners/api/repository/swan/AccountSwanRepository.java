package app.bpartners.api.repository.swan;

import app.bpartners.api.repository.swan.schema.SwanAccount;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AccountSwanRepository {
  List<SwanAccount> getAccounts();
}
