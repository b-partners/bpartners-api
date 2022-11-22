package app.bpartners.api.service;

import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.BusinessActivity;
import app.bpartners.api.model.CompanyInfo;
import app.bpartners.api.repository.AccountHolderRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AccountHolderService {

  private final AccountHolderRepository accountHolderRepository;
  private final BusinessActivityService businessActivityService;

  public List<AccountHolder> getAccountHoldersByAccountId(String accountId) {
    return accountHolderRepository.findAllByAccountId(accountId);
  }

  public AccountHolder getByBearerAndAccountId(String bearer, String accountId) {
    return accountHolderRepository.findAllByBearerAndAccountId(bearer, accountId).get(0);
  }

  public AccountHolder getAccountHolderByAccountId(String accountId) {
    return accountHolderRepository.findAllByAccountId(accountId).get(0);
  }

  public AccountHolder updateCompanyInfo(String accountId, String accountHolderId,
                                         CompanyInfo companyInfo) {
    return accountHolderRepository.save(accountId, accountHolderId, companyInfo);
  }

  public AccountHolder updateBusinessActivities(
      String optionalAccountId,
      String accountHolderId,
      BusinessActivity businessActivity) {
    businessActivityService.save(businessActivity);
    return accountHolderRepository.getByIdAndAccountId(accountHolderId, optionalAccountId);
  }
}
