package app.bpartners.api.repository.mapper;

import app.bpartners.api.model.Account;
import app.bpartners.api.model.AccountHolder;
import app.bpartners.api.repository.fintecture.schema.Beneficiary;
import app.bpartners.api.repository.fintecture.schema.PaymentReq;
import app.bpartners.api.repository.fintecture.schema.PaymentUrl;
import app.bpartners.api.service.AccountHolderService;
import app.bpartners.api.service.AccountService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class FintectureMapper {
  private final AccountService accountService;
  private final AccountHolderService accountHolderService;

  public PaymentReq toFintecturePaymentReq(app.bpartners.api.model.PaymentReq domain) {
    Account authenticatedAccount = accountService.getAccounts().get(0);
    AccountHolder authenticatedAccountHolder = accountHolderService.getAccountHolders().get(0);

    Beneficiary beneficiary = new Beneficiary();
    beneficiary.name = authenticatedAccountHolder.getName();
    beneficiary.iban = authenticatedAccount.getIban();
    beneficiary.swift_bic = authenticatedAccount.getBic();
    beneficiary.street = authenticatedAccountHolder.getAddress();
    beneficiary.city = authenticatedAccountHolder.getCity();
    beneficiary.country = authenticatedAccountHolder.getCountry().substring(0, 2);
    beneficiary.zip = authenticatedAccountHolder.getPostalCode();

    PaymentReq.Meta meta = new PaymentReq.Meta();
    meta.psu_name = domain.getPayerEmail();
    meta.psu_email = domain.getPayerEmail();

    PaymentReq.Attributes attributes = new PaymentReq.Attributes();
    attributes.communication = domain.getLabel();
    attributes.amount = domain.getAmount().toString();
    attributes.beneficiary = beneficiary;

    PaymentReq.Data data = new PaymentReq.Data();
    data.attributes = attributes;

    PaymentReq paymentReq = new PaymentReq();
    paymentReq.meta = meta;
    paymentReq.data = data;

    return paymentReq;
  }

  public app.bpartners.api.model.PaymentUrl toDomain(
      PaymentUrl fintecturePaymentUrl, String successUrl) {
    return app.bpartners.api.model.PaymentUrl.builder()
        .redirectUrl(fintecturePaymentUrl.meta.url)
        .successUrl(successUrl)
        .build();
  }
}
