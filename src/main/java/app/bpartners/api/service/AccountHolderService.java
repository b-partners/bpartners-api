package app.bpartners.api.service;

import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.AnnualRevenueTarget;
import app.bpartners.api.model.BusinessActivity;
import app.bpartners.api.model.CompanyInfo;
import app.bpartners.api.repository.AccountHolderRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@AllArgsConstructor
public class AccountHolderService {
  private final AccountHolderRepository accountHolderRepository;
  private final BusinessActivityService businessActivityService;
  private final AnnualRevenueTargetService annualRevenueTargetService;

  public AccountHolder getById(String id) {
    return accountHolderRepository.findById(id);
  }

  public List<AccountHolder> getAll(String name, Integer page, Integer pageSize) {
    Pageable pageable = PageRequest.of(page, pageSize);
    return name == null ? accountHolderRepository.findAll(pageable)
        : accountHolderRepository.findAllByName(name, pageable);
  }

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
    AccountHolder toSave = accountHolder.toBuilder()
        .socialCapital(companyInfo == null || companyInfo.getSocialCapital() == null
            ? accountHolder.getSocialCapital()
            : companyInfo.getSocialCapital())
        .email(companyInfo == null || companyInfo.getEmail() == null
            ? accountHolder.getEmail()
            : companyInfo.getEmail())
        .website(companyInfo == null
            ? accountHolder.getWebsite()
            : companyInfo.getWebsite())
        .mobilePhoneNumber(companyInfo == null || companyInfo.getPhone() == null
            ? accountHolder.getMobilePhoneNumber()
            : companyInfo.getPhone())
        .subjectToVat(companyInfo == null
            ? accountHolder.isSubjectToVat()
            : companyInfo.isSubjectToVat())
        .location(companyInfo == null || companyInfo.getLocation() == null
            ? accountHolder.getLocation()
            : companyInfo.getLocation())
        .townCode(companyInfo == null || companyInfo.getTownCode() == null
            ? accountHolder.getTownCode()
            : companyInfo.getTownCode())
        .vatNumber(companyInfo == null || companyInfo.getTvaNumber() == null
            ? accountHolder.getVatNumber()
            : companyInfo.getTvaNumber())
        .build();
    return accountHolderRepository.save(toSave);
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

  public List<AccountHolder> findAllByIdUser(String idUser) {
    return accountHolderRepository.findAllByUserId(idUser);
  }

  @Transactional
  public AccountHolder findDefaultByIdUser(String idUser) {
    List<AccountHolder> accountHolders = findAllByIdUser(idUser);
    AccountHolder defaultAccountHolder = accountHolders.isEmpty() ? null
        : accountHolders.get(0);
    if (accountHolders.size() > 1) {
      log.warn("Multiple account holders not supported. "
          + defaultAccountHolder.describe() + " chosen by default.");
    }
    return defaultAccountHolder;
  }
}
