package app.bpartners.api.repository.swan;

import app.bpartners.api.repository.swan.schema.SwanAccount;
import java.util.List;

public interface AccountSwanRepository {
  List<SwanAccount> getAccounts();
}
