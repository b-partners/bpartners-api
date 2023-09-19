package app.bpartners.api.repository;

import app.bpartners.api.model.AccountHolder;
import java.util.List;
import org.springframework.data.domain.Pageable;

public interface AccountHolderRepository {
  List<AccountHolder> findAll(Pageable pageable);

  List<AccountHolder> findAllByName(String name, Pageable pageable);

  List<AccountHolder> findAllByAccountId(String accountId);

  List<AccountHolder> findAllByUserId(String userId);

  AccountHolder save(AccountHolder accountHolder);

  AccountHolder findById(String id);
}
