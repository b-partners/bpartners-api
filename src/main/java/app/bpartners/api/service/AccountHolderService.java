package app.bpartners.api.service;

import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.AnnualRevenueTarget;
import app.bpartners.api.model.BusinessActivity;
import app.bpartners.api.model.CompanyInfo;
import app.bpartners.api.repository.AccountHolderRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@AllArgsConstructor
public class AccountHolderService {
  private final AccountHolderRepository accountHolderRepository;
  private final BusinessActivityService businessActivityService;
  private final AnnualRevenueTargetService annualRevenueTargetService;

  public List<AccountHolder> getAccountHoldersByAccountId(String accountId) {
    return accountHolderRepository.findAllByAccountId(accountId);
  }

  public AccountHolder getDefaultByAccountId(String accountId) {
    List<AccountHolder> accountHolders = accountHolderRepository.findAllByAccountId(accountId);
    AccountHolder defaultAccountHolder = accountHolders.get(0);
    if (accountHolders.size() > 1) {
      log.warn("Multiple account holders not supported. "
          + defaultAccountHolder.describe() + " chosen by default.");
    }
    return defaultAccountHolder;
  }

  public AccountHolder updateCompanyInfo(String accountHolderId,
                                         CompanyInfo companyInfo) {
    AccountHolder accountHolder = accountHolderRepository.findById(accountHolderId);
    return accountHolderRepository.save(accountHolder.toBuilder()
        .socialCapital(companyInfo.getSocialCapital())
        .email(companyInfo.getEmail())
        .mobilePhoneNumber(companyInfo.getPhone())
        .subjectToVat(companyInfo.isSubjectToVat())
        .location(companyInfo.getLocation())
        .townCode(companyInfo.getTownCode())
        .build());
  }

  public AccountHolder updateGlobalInfo(AccountHolder accountHolder) {
    return accountHolderRepository.save(accountHolder);
  }

  //TODO: map this update with the companyInfoUpdate and refactor to save method only
  public AccountHolder updateBusinessActivities(
      String accountId,
      String accountHolderId,
      BusinessActivity businessActivity) {
    businessActivityService.save(businessActivity);
    return accountHolderRepository.findById(accountHolderId);
  }

  public AccountHolder updateAnnualRevenueTargets(
      String accountId, String accountHolderId, List<AnnualRevenueTarget> annualRevenueTargets) {
    annualRevenueTargetService.saveAll(annualRevenueTargets);
    return accountHolderRepository.findById(accountHolderId);
  }

  public AccountHolder updateFeedBackConfiguration(AccountHolder accountHolder) {
    return accountHolderRepository.save(accountHolder);
  }
}
