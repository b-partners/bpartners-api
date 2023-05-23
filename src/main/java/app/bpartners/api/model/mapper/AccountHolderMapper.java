package app.bpartners.api.model.mapper;

import app.bpartners.api.endpoint.rest.model.Geojson;
import app.bpartners.api.endpoint.rest.model.VerificationStatus;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.validator.AccountHolderValidator;
import app.bpartners.api.repository.jpa.model.HAccountHolder;
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
        .userId(entity.getIdUser())
        .verificationStatus(entity.getVerificationStatus())
        .name(entity.getName())
        .subjectToVat(entity.isSubjectToVat())
        .email(entity.getEmail())
        .mobilePhoneNumber(entity.getMobilePhoneNumber())
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
        .feedbackLink(entity.getFeedbackLink())
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
        .idUser(domain.getUserId())
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
        .feedbackLink(domain.getFeedbackLink())
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
