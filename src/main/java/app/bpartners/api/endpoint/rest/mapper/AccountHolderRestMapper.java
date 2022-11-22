package app.bpartners.api.endpoint.rest.mapper;


import app.bpartners.api.endpoint.rest.model.CompanyBusinessActivity;
import app.bpartners.api.endpoint.rest.model.CompanyInfo;
import app.bpartners.api.endpoint.rest.model.ContactAddress;
import app.bpartners.api.endpoint.rest.validator.AccountHolderValidator;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.BusinessActivity;
import app.bpartners.api.service.BusinessActivityService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AccountHolderRestMapper {

  private final AccountHolderValidator validator;
  private final BusinessActivityService businessActivityService;

  public app.bpartners.api.endpoint.rest.model.AccountHolder toRest(AccountHolder accountHolder) {
    app.bpartners.api.endpoint.rest.model.AccountHolder restAccountHolder =
        new app.bpartners.api.endpoint.rest.model.AccountHolder();
    BusinessActivity model = businessActivityService.findByAccountHolderId(accountHolder.getId());
    String primaryActivity = model != null ? model.getPrimaryActivity() : null;
    String secondaryActivity = model != null ? model.getSecondaryActivity() : null;

    restAccountHolder.setId(accountHolder.getId());
    restAccountHolder.setName(accountHolder.getName());
    restAccountHolder.setOfficialActivityName(accountHolder.getMainActivity());
    restAccountHolder.setSiren(accountHolder.getSiren());
    restAccountHolder.setVerificationStatus(accountHolder.getVerificationStatus());

    restAccountHolder.setCompanyInfo(new CompanyInfo()
        .email(accountHolder.getEmail())
        .phone(accountHolder.getMobilePhoneNumber())
        .tvaNumber(accountHolder.getTvaNumber())
        .socialCapital(accountHolder.getSocialCapital()));

    restAccountHolder.setContactAddress(new ContactAddress()
        .city(accountHolder.getCity())
        .address(accountHolder.getAddress())
        .postalCode(accountHolder.getPostalCode())
        .country(accountHolder.getCountry()));

    restAccountHolder.setBusinessActivities(
        new CompanyBusinessActivity()
            .primary(primaryActivity)
            .secondary(secondaryActivity)
    );

    // /!\ Deprecated : use contactAddress instead
    restAccountHolder.setAddress(accountHolder.getAddress());
    restAccountHolder.setPostalCode(accountHolder.getPostalCode());
    restAccountHolder.setCity(accountHolder.getCity());
    restAccountHolder.setCountry(accountHolder.getCountry());

    return restAccountHolder;
  }

  public AccountHolder toDomain(
      app.bpartners.api.endpoint.rest.model.AccountHolder restAccountHolder) {
    validator.accept(restAccountHolder);
    ContactAddress contactAddress = restAccountHolder.getContactAddress();
    return AccountHolder.builder()
        .id(restAccountHolder.getId())
        .name(restAccountHolder.getName())
        .city(contactAddress.getCity())
        .country(contactAddress.getCountry())
        .address(contactAddress.getAddress())
        .postalCode(contactAddress.getPostalCode())
        .build();
  }
}
