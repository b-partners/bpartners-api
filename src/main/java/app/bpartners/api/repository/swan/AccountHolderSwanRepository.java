package app.bpartners.api.repository.swan;

import app.bpartners.api.repository.swan.schema.AccountHolder;

import java.util.List;

public interface AccountHolderSwanRepository {
  List<AccountHolder> getAccountHolders();
}
