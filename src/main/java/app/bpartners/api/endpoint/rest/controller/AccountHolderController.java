package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.AccountHolderRestMapper;
import app.bpartners.api.endpoint.rest.mapper.BusinessActivityRestMapper;
import app.bpartners.api.endpoint.rest.mapper.CompanyInfoMapper;
import app.bpartners.api.endpoint.rest.mapper.MicroBusinessMapper;
import app.bpartners.api.endpoint.rest.model.AccountHolder;
import app.bpartners.api.endpoint.rest.model.CompanyBusinessActivity;
import app.bpartners.api.endpoint.rest.model.CompanyInfo;
import app.bpartners.api.endpoint.rest.model.UpdateMicroBusiness;
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
  private final MicroBusinessMapper microBusinessMapper;

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

  @PutMapping("/users/{userId}/accounts/{accountId}/accountHolders/{ahId}/microBusiness")
  public AccountHolder updateMicroBusiness(
      @RequestBody UpdateMicroBusiness microBusiness,
      @PathVariable("userId") String userId,
      @PathVariable("accountId") String accountId,
      @PathVariable("ahId") String accountHolderId) {
    return accountHolderMapper.toRest(
        accountHolderService.updateMicroBusiness(accountId, accountHolderId,
            microBusinessMapper.toDomain(microBusiness)
        )
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
            businessActivityMapper.toDomain(businessActivity)
        )
    );
  }
}
