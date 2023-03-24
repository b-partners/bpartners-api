package app.bpartners.api.repository;

import app.bpartners.api.model.AccountHolder;
import java.util.List;

public interface AccountHolderRepository {
  List<AccountHolder> findAllByAccountId(String accountId);

  AccountHolder save(AccountHolder accountHolder);

  AccountHolder getByIdAndAccountId(String id, String accountId);

  AccountHolder findById(String id);
}
