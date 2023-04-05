package app.bpartners.api.model.mapper;

import app.bpartners.api.endpoint.rest.model.Geojson;
import app.bpartners.api.endpoint.rest.model.VerificationStatus;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.Fraction;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.validator.AccountHolderValidator;
import app.bpartners.api.repository.jpa.model.HAccountHolder;
import app.bpartners.api.repository.swan.model.SwanAccountHolder;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import static app.bpartners.api.service.utils.FractionUtils.parseFraction;

@AllArgsConstructor
@Component
public class AccountHolderMapper {
  public static final String VERIFIED_STATUS = "Verified";
  public static final String PENDING_STATUS = "Pending";
  public static final String NOT_STARTED_STATUS = "NotStarted";
  public static final String WAITING_FOR_INFORMATION_STATUS = "WaitingForInformation";
  public static final String GEOJSON_TYPE_POINT = "Point";
  private final AccountHolderValidator accountHolderValidator;

  public AccountHolder toDomain(HAccountHolder entity) {
    Geojson location = null;
    if (entity.getLongitude() != null && entity.getLatitude() != null) {
      location = new Geojson()
          .type(GEOJSON_TYPE_POINT)
          .longitude(entity.getLongitude())
          .latitude(entity.getLatitude());
    }
    return AccountHolder.builder()
        .id(entity.getId())
        .verificationStatus(entity.getVerificationStatus())
        .name(entity.getName())
        .subjectToVat(entity.isSubjectToVat())
        .email(entity.getEmail())
        .mobilePhoneNumber(entity.getMobilePhoneNumber())
        .accountId(entity.getAccountId())
        .socialCapital(parseFraction(entity.getSocialCapital()).getCentsRoundUp())
        .vatNumber(entity.getVatNumber())
        .address(entity.getAddress())
        .city(entity.getCity())
        .country(entity.getCountry())
        .postalCode(entity.getPostalCode())
        .siren(entity.getRegistrationNumber())
        .mainActivity(entity.getBusinessActivity())
        .mainActivityDescription(entity.getBusinessActivityDescription())
        .initialCashflow(parseFraction(entity.getInitialCashflow()))
        .location(location)
        .prospectingPerimeter(entity.getProspectingPerimeter())
        .townCode(entity.getTownCode())
        .build();
  }

  public HAccountHolder toEntity(AccountHolder domain) {
    accountHolderValidator.accept(domain);
    Double longitude = null;
    Double latitude = null;
    if (domain.getLocation() != null
        && domain.getLocation().getLongitude() != null
        && domain.getLocation().getLatitude() != null) {
      longitude = domain.getLocation().getLongitude();
      latitude = domain.getLocation().getLatitude();
    }
    return HAccountHolder.builder()
        .id(domain.getId())
        .accountId(domain.getAccountId())
        .email(domain.getEmail())
        .subjectToVat(domain.isSubjectToVat())
        .vatNumber(domain.getVatNumber())
        .mobilePhoneNumber(domain.getMobilePhoneNumber())
        .socialCapital(String.valueOf(parseFraction(domain.getSocialCapital())))
        .initialCashflow(domain.getInitialCashflow().toString())
        .verificationStatus(domain.getVerificationStatus())
        .name(domain.getName())
        .registrationNumber(domain.getSiren())
        .businessActivity(domain.getMainActivity())
        .businessActivityDescription(domain.getMainActivityDescription())
        .address(domain.getAddress())
        .city(domain.getCity())
        .country(domain.getCountry())
        .postalCode(domain.getPostalCode())
        .longitude(longitude)
        .latitude(latitude)
        .prospectingPerimeter(domain.getProspectingPerimeter())
        .townCode(domain.getTownCode())
        .build();
  }

  public HAccountHolder toEntity(String accountId, SwanAccountHolder swanAccountHolder) {
    return HAccountHolder.builder()
        .id(swanAccountHolder.getId())
        .accountId(accountId)
        .subjectToVat(true) //By default, an account holder IS subject to vat
        .mobilePhoneNumber(null)
        .email(null)
        .verificationStatus(getStatus(swanAccountHolder.getVerificationStatus()))
        .socialCapital(
            String.valueOf(new Fraction())) //TODO : check default social capital 0 or null
        .vatNumber(swanAccountHolder.getInfo().getVatNumber())
        .name(swanAccountHolder.getInfo().getName())
        .businessActivity(swanAccountHolder.getInfo().getBusinessActivity())
        .businessActivityDescription(swanAccountHolder.getInfo().getBusinessActivityDescription())
        .registrationNumber(swanAccountHolder.getInfo().getRegistrationNumber())
        .address(swanAccountHolder.getResidencyAddress().getAddressLine1())
        .city(swanAccountHolder.getResidencyAddress().getCity())
        .country(swanAccountHolder.getResidencyAddress().getCountry())
        .postalCode(swanAccountHolder.getResidencyAddress().getPostalCode())
        .initialCashflow(String.valueOf(0))
        .build();
  }

  public VerificationStatus getStatus(String value) {
    switch (value) {
      case VERIFIED_STATUS:
        return VerificationStatus.VERIFIED;
      case PENDING_STATUS:
        return VerificationStatus.PENDING;
      case NOT_STARTED_STATUS:
        return VerificationStatus.NOT_STARTED;
      case WAITING_FOR_INFORMATION_STATUS:
        return VerificationStatus.WAITING_FOR_INFORMATION;
      default:
        throw new NotFoundException("Unknown verification status " + value);
    }
  }
}
