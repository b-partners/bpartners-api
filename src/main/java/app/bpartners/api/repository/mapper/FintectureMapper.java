package app.bpartners.api.repository.mapper;

import app.bpartners.api.model.Account;
import app.bpartners.api.repository.fintecture.model.Beneficiary;
import app.bpartners.api.repository.fintecture.model.PaymentInitiation;
import app.bpartners.api.repository.fintecture.model.PaymentRedirection;
import app.bpartners.api.service.AccountService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class FintectureMapper {
  private final AccountService accountService;

  public PaymentInitiation toFintecturePaymentReq(
      app.bpartners.api.model.PaymentInitiation domain) {
    //TODO: GET users/id/accounts
    Account authenticatedAccount = accountService.getAccounts().get(0);

    Beneficiary beneficiary = new Beneficiary();
    beneficiary.setName(authenticatedAccount.getName());
    beneficiary.setIban(authenticatedAccount.getIban());
    beneficiary.setSwiftBic(authenticatedAccount.getBic());
    /*TODO
    beneficiary.street = authenticatedAccountHolder.getAddress();
    beneficiary.city = authenticatedAccountHolder.getCity();
    beneficiary.country = authenticatedAccountHolder.getCountry().substring(0, 2);
    beneficiary.zip = authenticatedAccountHolder.getPostalCode();
    */

    PaymentInitiation.Meta meta = new PaymentInitiation.Meta();
    meta.setPsuName(domain.getPayerEmail());
    meta.setPsuEmail(domain.getPayerEmail());

    PaymentInitiation.Attributes attributes = new PaymentInitiation.Attributes();
    attributes.setCommunication(domain.getLabel());
    attributes.setAmount(domain.getAmount());
    attributes.setBeneficiary(beneficiary);

    PaymentInitiation.Data data = new PaymentInitiation.Data();
    data.setAttributes(attributes);

    PaymentInitiation paymentReq = new PaymentInitiation();
    paymentReq.setMeta(meta);
    paymentReq.setData(data);

    return paymentReq;
  }

  public app.bpartners.api.model.PaymentRedirection toDomain(
      PaymentRedirection paymentRedirection, String successUrl) {
    return app.bpartners.api.model.PaymentRedirection.builder()
        .redirectUrl(paymentRedirection.getMeta().getUrl())
        .successUrl(successUrl)
        .build();
  }
}
