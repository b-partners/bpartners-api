package app.bpartners.api.repository;

import app.bpartners.api.model.AccountHolder;
import java.util.List;

public interface AccountHolderRepository {
  List<AccountHolder> findAllByAccountId(String accountId);

  List<AccountHolder> findAllByBearerAndAccountId(String bearer, String accountId);

  AccountHolder save(AccountHolder accountHolder);

  AccountHolder getByIdAndAccountId(String id, String accountId);
}
