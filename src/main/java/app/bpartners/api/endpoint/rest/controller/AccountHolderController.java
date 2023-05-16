package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.AccountHolderRestMapper;
import app.bpartners.api.endpoint.rest.mapper.AnnualRevenueTargetRestMapper;
import app.bpartners.api.endpoint.rest.mapper.BusinessActivityRestMapper;
import app.bpartners.api.endpoint.rest.mapper.CompanyInfoMapper;
import app.bpartners.api.endpoint.rest.model.AccountHolder;
import app.bpartners.api.endpoint.rest.model.AccountHolderFeedback;
import app.bpartners.api.endpoint.rest.model.CompanyBusinessActivity;
import app.bpartners.api.endpoint.rest.model.CompanyInfo;
import app.bpartners.api.endpoint.rest.model.CreateAnnualRevenueTarget;
import app.bpartners.api.endpoint.rest.model.UpdateAccountHolder;
import app.bpartners.api.endpoint.rest.validator.CreateAnnualRevenueTargetValidator;
import app.bpartners.api.model.AnnualRevenueTarget;
import app.bpartners.api.service.AccountHolderService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@AllArgsConstructor
public class AccountHolderController {

  private final AccountHolderService accountHolderService;
  private final AccountHolderRestMapper accountHolderMapper;
  private final CompanyInfoMapper companyInfoMapper;
  private final BusinessActivityRestMapper businessActivityMapper;
  private final AnnualRevenueTargetRestMapper revenueTargetRestMapper;
  private final CreateAnnualRevenueTargetValidator revenueTargetValidator;

  @GetMapping("/users/{userId}/accounts/{accountId}/accountHolders")
  public List<AccountHolder> getAccountHolders(
      @PathVariable String userId,
      @PathVariable String accountId) {
    return accountHolderService.getAccountHoldersByAccountId(accountId).stream()
        .map(accountHolderMapper::toRest)
        .collect(Collectors.toUnmodifiableList());
  }

  @PutMapping("/users/{userId}/accounts/{accountId}/accountHolders/{ahId}/companyInfo")
  public AccountHolder updateCompanyInfo(
      @RequestBody CompanyInfo companyInfo,
      @PathVariable("userId") String userId,
      @PathVariable("accountId") String accountId,
      @PathVariable("ahId") String accountHolderId) {
    return accountHolderMapper.toRest(
        accountHolderService.updateCompanyInfo(accountId, accountHolderId,
            companyInfoMapper.toDomain(companyInfo)
        )
    );
  }

  @PutMapping("/users/{userId}/accounts/{accountId}/accountHolders/{ahId}/globalInfo")
  public AccountHolder updateGlobalInfo(
      @RequestBody UpdateAccountHolder globalInfo,
      @PathVariable("userId") String userId,
      @PathVariable("accountId") String accountId,
      @PathVariable("ahId") String accountHolderId) {
    app.bpartners.api.model.AccountHolder
        accountHolder = accountHolderMapper.toDomain(accountHolderId, accountId,
        globalInfo);
    return accountHolderMapper.toRest(
        accountHolderService.updateGlobalInfo(accountHolder)
    );
  }

  @PutMapping("/users/{userId}/accounts/{accountId}/accountHolders/{ahId}/businessActivities")
  public AccountHolder updateBusinessActivities(
      @RequestBody CompanyBusinessActivity businessActivity,
      @PathVariable("userId") String userId,
      @PathVariable("accountId") String accountId,
      @PathVariable("ahId") String accountHolderId
  ) {
    return accountHolderMapper.toRest(
        accountHolderService.updateBusinessActivities(accountId, accountHolderId,
            businessActivityMapper.toDomain(accountId, businessActivity)
        )
    );
  }

  @PutMapping("/users/{userId}/accounts/{accountId}/accountHolders/{ahId}/revenueTargets")
  public AccountHolder updateAnnualRevenueTarget(
      @PathVariable("userId") String userId,
      @PathVariable("accountId") String accountId,
      @PathVariable("ahId") String accountHolderId,
      @RequestBody List<CreateAnnualRevenueTarget> toCreate
  ) {
    revenueTargetValidator.accept(toCreate);
    List<AnnualRevenueTarget> toSave = toCreate.stream()
        .map(revenueTarget ->
            revenueTargetRestMapper.toDomain(accountHolderId, revenueTarget))
        .collect(Collectors.toUnmodifiableList());
    return accountHolderMapper.toRest(
        accountHolderService.updateAnnualRevenueTargets(accountId, accountHolderId, toSave));
  }

  @PutMapping("users/{userId}/accountHolders/{ahId}/feedback/configuration")
  public AccountHolder updateFeedbackConf(
      @PathVariable("userId") String userId,
      @PathVariable("ahId") String accountHolderId,
      @RequestBody AccountHolderFeedback toUpdate
  ) {
    return accountHolderMapper.toRest(
        accountHolderService.updateFeedBackConfiguration(
            accountHolderMapper.toDomain(accountHolderId, toUpdate)
        )
    );
  }
}
