package app.bpartners.api.endpoint.rest.mapper;


import app.bpartners.api.endpoint.rest.model.AccountHolder;
import app.bpartners.api.endpoint.rest.model.CompanyBusinessActivity;
import app.bpartners.api.endpoint.rest.model.CompanyInfo;
import app.bpartners.api.endpoint.rest.model.ContactAddress;
import app.bpartners.api.endpoint.rest.validator.AccountHolderValidator;
import app.bpartners.api.model.BusinessActivity;
import app.bpartners.api.service.BusinessActivityService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AccountHolderRestMapper {

  private final AccountHolderValidator validator;
  private final BusinessActivityService businessActivityService;

  public AccountHolder toRest(app.bpartners.api.model.AccountHolder domain) {
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
        .companyInfo(new CompanyInfo()
            .email(domain.getEmail())
            .phone(domain.getMobilePhoneNumber())
            .tvaNumber(domain.getTvaNumber())
            .socialCapital(domain.getSocialCapital()))
        .contactAddress(new ContactAddress()
            .city(domain.getCity())
            .address(domain.getAddress())
            .postalCode(domain.getPostalCode())
            .country(domain.getCountry()))
        .businessActivities(new CompanyBusinessActivity()
            .primary(primaryActivity)
            .secondary(secondaryActivity))
        // /!\ Deprecated : use contactAddress instead
        .address(domain.getAddress())
        .postalCode(domain.getPostalCode())
        .city(domain.getCity())
        .country(domain.getCountry());
  }
}
