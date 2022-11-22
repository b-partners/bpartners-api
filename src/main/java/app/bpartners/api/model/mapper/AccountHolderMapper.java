package app.bpartners.api.model.mapper;

import app.bpartners.api.endpoint.rest.model.VerificationStatus;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.repository.jpa.model.HAccountHolder;
import org.springframework.stereotype.Component;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;
import static app.bpartners.api.service.utils.FractionUtils.parseFraction;

@Component
public class AccountHolderMapper {
  public static final String VERIFIED_STATUS = "Verified";
  public static final String PENDING_STATUS = "Pending";
  public static final String NOT_STARTED_STATUS = "NotStarted";
  public static final String WAITING_FOR_INFORMATION_STATUS = "WaitingForInformation";

  public AccountHolder toDomain(
      app.bpartners.api.repository.swan.model.AccountHolder accountHolder, HAccountHolder entity) {
    return AccountHolder.builder()
        .id(accountHolder.getId())
        .verificationStatus(getVerificationStatus(accountHolder.getVerificationStatus()))
        .name(accountHolder.getInfo().getName())
        .email(entity.getEmail())
        .mobilePhoneNumber(entity.getMobilePhoneNumber())
        .accountId(entity.getAccountId())
        .socialCapital(entity.getSocialCapital())
        .tvaNumber(entity.getTvaNumber())
        .address(accountHolder.getResidencyAddress().getAddressLine1())
        .city(accountHolder.getResidencyAddress().getCity())
        .country(accountHolder.getResidencyAddress().getCountry())
        .postalCode(accountHolder.getResidencyAddress().getPostalCode())
        .siren(accountHolder.getInfo().getRegistrationNumber())
        .mainActivity(accountHolder.getInfo().getBusinessActivity())
        .mainActivityDescription(accountHolder.getInfo().getBusinessActivityDescription())
        .initialCashflow(parseFraction(entity.getInitialCashflow()))
        .build();
  }

  private VerificationStatus getVerificationStatus(String status) {
    switch (status) {
      case VERIFIED_STATUS:
        return VerificationStatus.VERIFIED;
      case PENDING_STATUS:
        return VerificationStatus.PENDING;
      case NOT_STARTED_STATUS:
        return VerificationStatus.NOT_STARTED;
      case WAITING_FOR_INFORMATION_STATUS:
        return VerificationStatus.WAITING_FOR_INFORMATION;
      default:
        throw new ApiException(SERVER_EXCEPTION, "Unknown status " + status);
    }
  }
}
