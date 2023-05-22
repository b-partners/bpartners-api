package app.bpartners.api.repository.mapper;

import app.bpartners.api.endpoint.rest.security.AuthenticatedResourceProvider;
import app.bpartners.api.model.Account;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.model.PaymentInitiation;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.repository.fintecture.model.Beneficiary;
import app.bpartners.api.repository.fintecture.model.FPaymentInitiation;
import app.bpartners.api.repository.fintecture.model.FPaymentRedirection;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class FintectureMapper {
  private final AuthenticatedResourceProvider authResourceProvider;

  private Beneficiary toBeneficiary(Account account, AccountHolder accountHolder) {
    return Beneficiary.builder()
        .name(account.getName())
        .iban(account.getIban())
        .swiftBic(account.getBic())
        .street(accountHolder.getAddress())
        .city(accountHolder.getCity())
        .country(accountHolder.getCountry().substring(0, 2))
        .zip(accountHolder.getPostalCode()).build();
  }

  //TODO: put these checks properly
  public FPaymentInitiation toFintectureResource(PaymentInitiation domain) {
    Account authenticatedAccount = authResourceProvider.getAccount();
    AccountHolder authenticatedAccountHolder = authResourceProvider.getDefaultAccountHolder();
    if (authenticatedAccount.getIban() == null) {
      throw new BadRequestException(
          "Account("
              + "id=" + authenticatedAccount.getId()
              + ", name=" + authenticatedAccount.getName() + ") "
              + "does not have iban. Iban is mandatory to initiate payment");
    }
    if (authenticatedAccount.getBic() == null) {
      throw new BadRequestException(
          "Account("
              + "id=" + authenticatedAccount.getId()
              + ", name=" + authenticatedAccount.getName() + ") "
              + "does not have bic. Bic is mandatory to initiate payment");
    }
    Beneficiary beneficiary = toBeneficiary(authenticatedAccount, authenticatedAccountHolder);

    FPaymentInitiation.Attributes attributes = new FPaymentInitiation.Attributes();
    attributes.setAmount(String.valueOf(domain.getAmount().getCentsAsDecimal()));
    attributes.setBeneficiary(beneficiary);
    attributes.setCommunication(domain.getLabel());

    FPaymentInitiation.Meta meta = new FPaymentInitiation.Meta();
    meta.setPsuName(domain.getPayerName());
    meta.setPsuEmail(domain.getPayerEmail());

    FPaymentInitiation.Data data = new FPaymentInitiation.Data();
    data.setAttributes(attributes);

    return new FPaymentInitiation(meta, data);
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
