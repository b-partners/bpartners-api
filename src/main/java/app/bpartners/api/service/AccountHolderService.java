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

  public List<AccountHolder> getAccountHolders(String acccountId) {
    return accountHolderRepository.getByAccountId(acccountId);
  }

  public AccountHolder getAccountHolderByAccountId(String accountId) {
    return accountHolderRepository.getByAccountId(accountId).get(0);
  }

  public AccountHolder updateCompanyInfo(String id, CompanyInfo companyInfo) {
    return accountHolderRepository.save(id, companyInfo);
  }

  public AccountHolder updateBusinessActivities(String id, BusinessActivity businessActivity) {
    businessActivityService.save(businessActivity);
    return accountHolderRepository.getById(id);
  }
}
