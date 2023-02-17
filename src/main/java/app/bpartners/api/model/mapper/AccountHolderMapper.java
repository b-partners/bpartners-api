package app.bpartners.api.model.mapper;

import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.repository.jpa.model.HAccountHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static app.bpartners.api.service.utils.FractionUtils.parseFraction;

@Slf4j
@Component
public class AccountHolderMapper {
  public static final String VERIFIED_STATUS = "Verified";
  public static final String PENDING_STATUS = "Pending";
  public static final String NOT_STARTED_STATUS = "NotStarted";
  public static final String WAITING_FOR_INFORMATION_STATUS = "WaitingForInformation";

  public AccountHolder toDomain(HAccountHolder entity) {
    return AccountHolder.builder()
        .id(entity.getId())
        .verificationStatus(entity.getVerificationStatus())
        .name(entity.getName())
        .subjectToVat(entity.isSubjectToVat())
        .email(entity.getEmail())
        .mobilePhoneNumber(entity.getMobilePhoneNumber())
        .accountId(entity.getAccountId())
        .socialCapital(entity.getSocialCapital())
        .vatNumber(entity.getVatNumber())
        .address(entity.getAddress())
        .city(entity.getCity())
        .country(entity.getCountry())
        .postalCode(entity.getPostalCode())
        .siren(entity.getRegistrationNumber())
        .mainActivity(entity.getBusinessActivity())
        .mainActivityDescription(entity.getBusinessActivityDescription())
        .initialCashflow(parseFraction(entity.getInitialCashflow()))
        .build();
  }

  public HAccountHolder toEntity(AccountHolder domain) {
    return HAccountHolder.builder()
        .id(domain.getId())
        .accountId(domain.getAccountId())
        .email(domain.getEmail())
        .subjectToVat(domain.isSubjectToVat())
        .vatNumber(domain.getVatNumber())
        .mobilePhoneNumber(domain.getMobilePhoneNumber())
        .socialCapital(domain.getSocialCapital())
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
        .build();
  }
}
