package app.bpartners.api.repository.swan;

import app.bpartners.api.repository.swan.model.AccountHolder;
import java.util.List;

public interface AccountHolderSwanRepository {
  List<AccountHolder> getAccountHoldersByAccountId(String accountId);

  AccountHolder getById(String id);
}
