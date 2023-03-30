package app.bpartners.api.service;

import app.bpartners.api.endpoint.rest.model.UpdateAccountHolder;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.AnnualRevenueTarget;
import app.bpartners.api.model.BusinessActivity;
import app.bpartners.api.model.CompanyInfo;
import app.bpartners.api.repository.AccountHolderRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import static app.bpartners.api.service.utils.FractionUtils.parseFraction;

@Service
@AllArgsConstructor
public class AccountHolderService {
  private final AccountHolderRepository accountHolderRepository;
  private final BusinessActivityService businessActivityService;
  private final AnnualRevenueTargetService annualRevenueTargetService;

  public List<AccountHolder> getAccountHoldersByAccountId(String accountId) {
    return accountHolderRepository.findAllByAccountId(accountId);
  }

  public AccountHolder getAccountHolderByAccountId(String accountId) {
    return accountHolderRepository.findAllByAccountId(accountId).get(0);
  }

  public AccountHolder updateCompanyInfo(String accountId, String accountHolderId,
                                         CompanyInfo companyInfo) {
    AccountHolder accountHolder = accountHolderRepository.getByIdAndAccountId(accountHolderId,
        accountId);
    return accountHolderRepository.save(accountHolder.toBuilder()
        .socialCapital(companyInfo.getSocialCapital())
        .email(companyInfo.getEmail())
        .mobilePhoneNumber(companyInfo.getPhone())
        .subjectToVat(companyInfo.isSubjectToVat())
        .location(companyInfo.getLocation())
        .townCode(companyInfo.getTownCode())
        .build());
  }

  public AccountHolder updateGlobalInfo(String accountId, String accountHolderId,
                                        UpdateAccountHolder global) {
    AccountHolder accountHolder = accountHolderRepository.getByIdAndAccountId(accountHolderId,
        accountId);
    return accountHolderRepository.save(accountHolder.toBuilder()
        .name(global.getName())
        .siren(global.getSiren())
        .initialCashflow(parseFraction(global.getInitialCashFlow()))
        .mainActivity(global.getOfficialActivityName())
        .address(global.getContactAddress().getAddress())
        .city(global.getContactAddress().getCity())
        .postalCode(global.getContactAddress().getPostalCode())
        .country(global.getContactAddress().getCountry())
        .build());
  }

  //TODO: map this update with the companyInfoUpdate and refactor to save method only
  public AccountHolder updateBusinessActivities(
      String optionalAccountId,
      String accountHolderId,
      BusinessActivity businessActivity) {
    businessActivityService.save(businessActivity);
    return accountHolderRepository.getByIdAndAccountId(accountHolderId, optionalAccountId);
  }

  public AccountHolder updateAnnualRevenueTargets(
      String optionalAccountId,
      String accountHolderId,
      List<AnnualRevenueTarget> annualRevenueTargets) {
    annualRevenueTargetService.save(annualRevenueTargets);
    return accountHolderRepository.getByIdAndAccountId(accountHolderId, optionalAccountId);
  }
}
