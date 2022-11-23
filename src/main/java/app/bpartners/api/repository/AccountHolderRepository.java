package app.bpartners.api.repository;

import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.CompanyInfo;
import java.util.List;

public interface AccountHolderRepository {
  List<AccountHolder> findAllByAccountId(String accountId);

  List<AccountHolder> findAllByBearerAndAccountId(String bearer, String accountId);

  AccountHolder save(String accountId, String accountHolderId, CompanyInfo companyInfo);

  AccountHolder getByIdAndAccountId(String id, String accountId);
}
