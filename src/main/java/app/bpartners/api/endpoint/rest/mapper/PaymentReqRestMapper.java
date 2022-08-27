package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.PaymentInitiation;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import org.springframework.stereotype.Component;

@Component
public class PaymentReqRestMapper {

  public app.bpartners.api.model.PaymentInitiation toDomain(PaymentInitiation rest) {
    RedirectionStatusUrls statusUrls = rest.getRedirectionStatusUrls();
    return app.bpartners.api.model.PaymentInitiation.builder()
        .id(rest.getId())
        .label(rest.getLabel())
        .reference(rest.getReference())
        .amount(rest.getAmount().doubleValue()) //TODO: nooooooo
        .payerEmail(rest.getPayerEmail())
        .payerName(rest.getPayerName())
        .successUrl(statusUrls.getSuccessUrl())
        .failureUrl(statusUrls.getFailureUrl())
        .build();
  }
}
