package app.bpartners.api.repository.mapper;

import app.bpartners.api.model.Account;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.PaymentInitiation;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.repository.fintecture.model.Beneficiary;
import app.bpartners.api.repository.fintecture.model.FPaymentInitiation;
import app.bpartners.api.repository.fintecture.model.FPaymentRedirection;
import app.bpartners.api.service.utils.PatternMatcher;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
@Slf4j
public class FintectureMapper {
  public static final String CUSTOM_COMMUNICATION_PATTERN =
      "[^a-zA-Z0-9 éèêëçâùûôî.,;:/!?$%_=+()*°@#ÂÇÉÈÊËÎÔÙÛ]";

  private Beneficiary toBeneficiary(Account account, AccountHolder accountHolder) {
    checkMandatoryHolderInfos(accountHolder);
    return Beneficiary.builder()
        .name(account.getName())
        .iban(account.getIban())
        .swiftBic(account.getBic())
        .street(accountHolder.getAddress())
        .city(accountHolder.getCity())
        .country(accountHolder.getCountry().substring(0, 2))
        .zip(accountHolder.getPostalCode())
        .build();
  }

  private void checkMandatoryHolderInfos(AccountHolder accountHolder) {
    StringBuilder builder = new StringBuilder();
    if (accountHolder.getAddress() == null) {
      builder.append("Account holder address is mandatory to initiate payment");
    }
    if (accountHolder.getCountry() == null) {
      builder.append("Account holder country is mandatory to initiate payment");
    }
    if (accountHolder.getCity() == null) {
      builder.append("Account holder city is mandatory to initiate payment");
    }
    if (accountHolder.getPostalCode() == null) {
      builder.append("Account holder postal code is mandatory to initiate payment");
    }
    String message = builder.toString();
    if (!message.isEmpty()) {
      throw new BadRequestException(message);
    }
  }

  // TODO: put these checks properly
  public FPaymentInitiation toFintectureResource(
      PaymentInitiation domain, Account account, AccountHolder accountHolder) {
    if (account.getIban() == null) {
      throw new BadRequestException(
          "Account("
              + "id="
              + account.getId()
              + ", name="
              + account.getName()
              + ") "
              + "does not have iban. Iban is mandatory to initiate payment");
    }
    if (account.getBic() == null) {
      throw new BadRequestException(
          "Account("
              + "id="
              + account.getId()
              + ", name="
              + account.getName()
              + ") "
              + "does not have bic. Bic is mandatory to initiate payment");
    }
    Beneficiary beneficiary = toBeneficiary(account, accountHolder);

    FPaymentInitiation.Attributes attributes = new FPaymentInitiation.Attributes();
    attributes.setAmount(String.valueOf(domain.getAmount().getCentsAsDecimal()));
    attributes.setBeneficiary(beneficiary);
    attributes.setCommunication(getCommunicationValue(domain));

    FPaymentInitiation.Meta meta = new FPaymentInitiation.Meta();
    meta.setPsuName(domain.getPayerName());
    meta.setPsuEmail(domain.getPayerEmail());

    FPaymentInitiation.Data data = new FPaymentInitiation.Data();
    data.setAttributes(attributes);

    return new FPaymentInitiation(meta, data);
  }

  private String getCommunicationValue(PaymentInitiation domain) {
    if (domain.getLabel() == null || domain.getLabel().length() < 3) {
      log.warn(
          "Payment label "
              + domain.getLabel()
              + " was null or < 3 chars. "
              + "EMPTY_LABEL set by default for "
              + domain);
      return "EMPTY_LABEL";
    }
    return PatternMatcher.filterCharacters(domain.getLabel(), CUSTOM_COMMUNICATION_PATTERN);
  }

  public app.bpartners.api.model.PaymentRedirection toDomain(
      FPaymentRedirection redirection, PaymentInitiation paymentInitiation) {
    if (redirection == null) {
      return null;
    }
    return app.bpartners.api.model.PaymentRedirection.builder()
        .endToEndId(paymentInitiation.getId())
        .sessionId(redirection.getMeta().getSessionId())
        .redirectUrl(redirection.getMeta().getUrl())
        .successUrl(paymentInitiation.getSuccessUrl())
        .failureUrl(paymentInitiation.getFailureUrl())
        .build();
  }
}
