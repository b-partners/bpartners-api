package app.bpartners.api.repository.mapper;

import app.bpartners.api.endpoint.rest.security.AuthenticatedResourceProvider;
import app.bpartners.api.model.Account;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.repository.fintecture.model.Beneficiary;
import app.bpartners.api.repository.fintecture.model.PaymentInitiation;
import app.bpartners.api.repository.fintecture.model.PaymentRedirection;
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

  public PaymentInitiation toFintecturePaymentReq(
      app.bpartners.api.model.PaymentInitiation domain) {
    Account authenticatedAccount = authResourceProvider.getAccount();
    AccountHolder authenticatedAccountHolder = authResourceProvider.getAccountHolder();

    Beneficiary beneficiary = toBeneficiary(authenticatedAccount, authenticatedAccountHolder);

    PaymentInitiation.Attributes attributes = new PaymentInitiation.Attributes();
    attributes.setAmount(String.valueOf(domain.getAmount().getCentsAsDecimal()));
    attributes.setBeneficiary(beneficiary);
    attributes.setCommunication(domain.getLabel());

    PaymentInitiation.Meta meta = new PaymentInitiation.Meta();
    meta.setPsuName(domain.getPayerName());
    meta.setPsuEmail(domain.getPayerEmail());

    PaymentInitiation.Data data = new PaymentInitiation.Data();
    data.setAttributes(attributes);

    PaymentInitiation paymentReq = new PaymentInitiation();
    paymentReq.setMeta(meta);
    paymentReq.setData(data);

    return paymentReq;
  }

  public app.bpartners.api.model.PaymentRedirection toDomain(
      PaymentRedirection paymentRedirection,
      app.bpartners.api.model.PaymentInitiation paymentInitiation) {
    return app.bpartners.api.model.PaymentRedirection.builder()
        .id(paymentInitiation.getId())
        .sessionId(paymentRedirection.getMeta().getSessionId())
        .redirectUrl(paymentRedirection.getMeta().getUrl())
        .successUrl(paymentInitiation.getSuccessUrl())
        .failureUrl(paymentInitiation.getFailureUrl())
        .build();
  }
}
