package app.bpartners.api.repository.swan;

import app.bpartners.api.repository.swan.model.AccountHolder;
import java.util.List;

public interface AccountHolderSwanRepository {
  List<AccountHolder> findAllByAccountId(String accountId);

  List<AccountHolder> findAllByBearerAndAccountId(String bearer, String accountId);

  AccountHolder getById(String id);
}
