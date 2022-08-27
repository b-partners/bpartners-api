package app.bpartners.api.repository.mapper;

import app.bpartners.api.model.Account;
import app.bpartners.api.model.User;
import app.bpartners.api.repository.fintecture.schema.Beneficiary;
import app.bpartners.api.repository.fintecture.schema.PaymentInitiation;
import app.bpartners.api.repository.fintecture.schema.PaymentRedirection;
import app.bpartners.api.service.AccountService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class FintectureMapper {
  private final AccountService accountService;

  public PaymentInitiation toFintecturePaymentReq(app.bpartners.api.model.PaymentInitiation domain) {
    Account authenticatedAccount = accountService.getAccounts().get(0);
    User user = new User(); //TODO: retrieve from userService

    Beneficiary beneficiary = new Beneficiary();
    beneficiary.name = user.getSwanUser().getLastName();
    beneficiary.iban = authenticatedAccount.getIban();
    beneficiary.swift_bic = authenticatedAccount.getBic();
    /*TODO
    beneficiary.street = authenticatedAccountHolder.getAddress();
    beneficiary.city = authenticatedAccountHolder.getCity();
    beneficiary.country = authenticatedAccountHolder.getCountry().substring(0, 2);
    beneficiary.zip = authenticatedAccountHolder.getPostalCode();
    */

    PaymentInitiation.Meta meta = new PaymentInitiation.Meta();
    meta.psu_name = domain.getPayerEmail();
    meta.psu_email = domain.getPayerEmail();

    PaymentInitiation.Attributes attributes = new PaymentInitiation.Attributes();
    attributes.communication = domain.getLabel();
    attributes.amount = domain.getAmount().toString();
    attributes.beneficiary = beneficiary;

    PaymentInitiation.Data data = new PaymentInitiation.Data();
    data.attributes = attributes;

    PaymentInitiation paymentReq = new PaymentInitiation();
    paymentReq.meta = meta;
    paymentReq.data = data;

    return paymentReq;
  }

  public app.bpartners.api.model.PaymentRedirection toDomain(
      PaymentRedirection fintecturePaymentUrl, String successUrl) {
    return app.bpartners.api.model.PaymentRedirection.builder()
        .redirectUrl(fintecturePaymentUrl.meta.url)
        .successUrl(successUrl)
        .build();
  }
}
