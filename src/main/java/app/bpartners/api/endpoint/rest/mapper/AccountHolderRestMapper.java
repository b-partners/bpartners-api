package app.bpartners.api.endpoint.rest.mapper;


import app.bpartners.api.endpoint.rest.model.AccountHolder;
import app.bpartners.api.endpoint.rest.model.AccountHolderFeedback;
import app.bpartners.api.endpoint.rest.model.AnnualRevenueTarget;
import app.bpartners.api.endpoint.rest.model.CompanyBusinessActivity;
import app.bpartners.api.endpoint.rest.model.CompanyInfo;
import app.bpartners.api.endpoint.rest.model.ContactAddress;
import app.bpartners.api.endpoint.rest.model.UpdateAccountHolder;
import app.bpartners.api.endpoint.rest.validator.AccountHolderRestValidator;
import app.bpartners.api.model.BusinessActivity;
import app.bpartners.api.repository.AccountHolderRepository;
import app.bpartners.api.service.AnnualRevenueTargetService;
import app.bpartners.api.service.BusinessActivityService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import static app.bpartners.api.service.utils.FractionUtils.parseFraction;

@Component
@AllArgsConstructor
public class AccountHolderRestMapper {

  private final AccountHolderRestValidator validator;
  private final BusinessActivityService businessActivityService;
  private final AnnualRevenueTargetService annualRevenueTargetService;
  private final AnnualRevenueTargetRestMapper annualRevenueTargetRestMapper;
  private final AccountHolderRepository accountHolderRepository;

  public AccountHolder toRest(app.bpartners.api.model.AccountHolder domain) {
    List<AnnualRevenueTarget> annualRevenueTargets =
        annualRevenueTargetService.getAnnualRevenueTargets(domain.getId()).stream()
            .map(annualRevenueTargetRestMapper::toRest)
            .collect(Collectors.toUnmodifiableList());

    BusinessActivity businessActivity =
        businessActivityService.findByAccountHolderId(domain.getId());
    String primaryActivity =
        businessActivity != null ? businessActivity.getPrimaryActivity() : null;
    String secondaryActivity =
        businessActivity != null ? businessActivity.getSecondaryActivity() : null;
    return new AccountHolder()
        .id(domain.getId())
        .name(domain.getName())
        .officialActivityName(domain.getMainActivity())
        .siren(domain.getSiren())
        .verificationStatus(domain.getVerificationStatus())
        .initialCashflow(domain.getInitialCashflow().getCentsRoundUp())
        .companyInfo(new CompanyInfo()
            .isSubjectToVat(domain.isSubjectToVat())
            .email(domain.getEmail())
            .phone(domain.getMobilePhoneNumber())
            .tvaNumber(domain.getVatNumber())
            .socialCapital(domain.getSocialCapital())
            .location(domain.getLocation())
            .townCode(domain.getTownCode()))
        .contactAddress(new ContactAddress()
            .prospectingPerimeter(domain.getProspectingPerimeter())
            .city(domain.getCity())
            .address(domain.getAddress())
            .postalCode(domain.getPostalCode())
            .country(domain.getCountry()))
        .businessActivities(new CompanyBusinessActivity()
            .primary(primaryActivity)
            .secondary(secondaryActivity))
        .revenueTargets(annualRevenueTargets)
        // /!\ Deprecated : use contactAddress instead
        .address(domain.getAddress())
        .postalCode(domain.getPostalCode())
        .city(domain.getCity())
        .country(domain.getCountry())
        .feedback(createAccountHolderFeedback(domain.getFeedbackLink()));
  }

  public AccountHolderFeedback createAccountHolderFeedback(String feedbackLink) {
    return new AccountHolderFeedback()
        .feedbackLink(feedbackLink);
  }

  public app.bpartners.api.model.AccountHolder toDomain(
      String accountHolderId, String accountId, UpdateAccountHolder global) {
    app.bpartners.api.model.AccountHolder accountHolder =
        accountHolderRepository.getByIdAndAccountId(accountHolderId, accountId);
    return accountHolder.toBuilder()
        .name(global.getName())
        .siren(global.getSiren())
        .initialCashflow(parseFraction(global.getInitialCashFlow()))
        .mainActivity(global.getOfficialActivityName())
        .address(global.getContactAddress().getAddress())
        .city(global.getContactAddress().getCity())
        .postalCode(global.getContactAddress().getPostalCode())
        .country(global.getContactAddress().getCountry())
        .prospectingPerimeter(global.getContactAddress().getProspectingPerimeter())
        .build();
  }

  public app.bpartners.api.model.AccountHolder toDomain(
      String accountHolderId,
      AccountHolderFeedback accountHolderFeedback
  ) {
    app.bpartners.api.model.AccountHolder accountHolder =
        accountHolderRepository.findById(accountHolderId);
    return accountHolder.toBuilder()
        .feedbackLink(accountHolderFeedback.getFeedbackLink())
        .build();
  }
}
