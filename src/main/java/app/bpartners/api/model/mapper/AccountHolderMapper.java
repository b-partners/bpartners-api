package app.bpartners.api.model.mapper;

import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.repository.jpa.model.HAccountHolder;
import org.springframework.stereotype.Component;

@Component
public class AccountHolderMapper {
  public AccountHolder toDomain(
      app.bpartners.api.repository.swan.model.AccountHolder accountHolder, HAccountHolder entity) {
    return AccountHolder.builder()
        .id(accountHolder.getId())
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
        .build();
  }
}
