package app.bpartners.api.repository;

import app.bpartners.api.model.AccountHolder;
import java.util.List;

public interface AccountHolderRepository {
  List<AccountHolder> getAccountHolders();
}
