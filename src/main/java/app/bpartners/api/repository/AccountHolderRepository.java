package app.bpartners.api.repository;

import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.CompanyInfo;
import java.util.List;

public interface AccountHolderRepository {
  List<AccountHolder> getByAccountId(String accountId);

  AccountHolder save(String accountHolderId, CompanyInfo companyInfo);

  AccountHolder getById(String id);
}
