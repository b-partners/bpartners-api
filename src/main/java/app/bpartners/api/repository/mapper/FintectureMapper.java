package app.bpartners.api.repository.mapper;

import app.bpartners.api.endpoint.rest.security.model.Principal;
import app.bpartners.api.endpoint.rest.security.principal.PrincipalProvider;
import app.bpartners.api.model.Account;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.repository.fintecture.model.Beneficiary;
import app.bpartners.api.repository.fintecture.model.PaymentInitiation;
import app.bpartners.api.repository.fintecture.model.PaymentRedirection;
import app.bpartners.api.service.AccountHolderService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class FintectureMapper {
  private final AccountHolderService accountHolderService;
  private final PrincipalProvider auth;

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
    Account authenticatedAccount = getAuthenticatedAccount();
    AccountHolder authenticatedAccountHolder = accountHolderService
        .getAccountHolderByAccountId(authenticatedAccount.getId());

    Beneficiary beneficiary = toBeneficiary(authenticatedAccount, authenticatedAccountHolder);

    PaymentInitiation.Attributes attributes = new PaymentInitiation.Attributes();
    attributes.setCommunication(domain.getLabel());
    attributes.setAmount(domain.getAmount().getCentsOfApproximatedValue());
    attributes.setBeneficiary(beneficiary);

    PaymentInitiation.Meta meta = new PaymentInitiation.Meta();
    meta.setPsuName(domain.getPayerEmail());
    meta.setPsuEmail(domain.getPayerEmail());

    PaymentInitiation.Data data = new PaymentInitiation.Data();
    data.setAttributes(attributes);

    PaymentInitiation paymentReq = new PaymentInitiation();
    paymentReq.setMeta(meta);
    paymentReq.setData(data);

    return paymentReq;
  }

  public app.bpartners.api.model.PaymentRedirection toDomain(
      PaymentRedirection fintecturePaymentUrl, String successUrl) {
    return app.bpartners.api.model.PaymentRedirection.builder()
        .redirectUrl(fintecturePaymentUrl.getMeta().getUrl()).successUrl(successUrl).build();
  }

  private Account getAuthenticatedAccount() {
    return ((Principal) auth.getAuthentication().getPrincipal()).getAccount();
  }

}
