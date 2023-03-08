package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.CreatePaymentRegulation;
import app.bpartners.api.endpoint.rest.model.PaymentInitiation;
import app.bpartners.api.endpoint.rest.model.PaymentRedirection;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.endpoint.rest.validator.PaymentInitValidator;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import static app.bpartners.api.service.utils.FractionUtils.parseFraction;

@Component
@AllArgsConstructor
public class PaymentInitRestMapper {
  private final PaymentInitValidator paymentValidator;

  public app.bpartners.api.model.PaymentInitiation toDomain(PaymentInitiation rest) {
    RedirectionStatusUrls statusUrls = rest.getRedirectionStatusUrls();
    paymentValidator.accept(rest);
    return app.bpartners.api.model.PaymentInitiation.builder()
        .id(rest.getId())
        .label(rest.getLabel())
        .reference(rest.getReference())
        .amount(parseFraction(rest.getAmount()))
        .payerEmail(rest.getPayerEmail())
        .payerName(rest.getPayerName())
        .successUrl(statusUrls.getSuccessUrl())
        .failureUrl(statusUrls.getFailureUrl())
        .build();
  }

  public app.bpartners.api.model.CreatePaymentRegulation toDomain(
      CreatePaymentRegulation payments) {
    if (payments == null) {
      return null;
    }
    return app.bpartners.api.model.CreatePaymentRegulation.builder()
        .amount(parseFraction(payments.getAmount()))
        .comment(payments.getComment())
        .percent(parseFraction(payments.getPercent()))
        .maturityDate(payments.getMaturityDate())
        .build();
  }

  public PaymentRedirection toRest(app.bpartners.api.model.PaymentRedirection domain) {
    RedirectionStatusUrls statusUrls = new RedirectionStatusUrls()
        .successUrl(domain.getSuccessUrl())
        .failureUrl(domain.getFailureUrl());
    return new PaymentRedirection()
        .id(domain.getEndToEndId())
        .redirectionUrl(domain.getRedirectUrl())
        .redirectionStatusUrls(statusUrls);
  }
}
