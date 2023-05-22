package app.bpartners.api.repository;

import app.bpartners.api.model.AccountHolder;
import java.util.List;

public interface AccountHolderRepository {
  List<AccountHolder> findAllByAccountId(String accountId);

  List<AccountHolder> findAllByUserId(String userId);

  AccountHolder save(AccountHolder accountHolder);

  AccountHolder findById(String id);
}
