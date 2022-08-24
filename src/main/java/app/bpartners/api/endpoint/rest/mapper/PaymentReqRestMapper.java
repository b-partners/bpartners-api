package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.PaymentReq;
import org.springframework.stereotype.Component;

@Component
public class PaymentReqRestMapper {

  public app.bpartners.api.model.PaymentReq toDomain(PaymentReq rest) {
    return app.bpartners.api.model.PaymentReq.builder()
        .id(rest.getId())
        .label(rest.getLabel())
        .reference(rest.getReference())
        .amount(rest.getAmount().doubleValue())
        .payerEmail(rest.getPayerEmail())
        .payerName(rest.getPayerName())
        .successUrl(rest.getSuccessUrl())
        .failureUrl(rest.getFailureUrl())
        .build();
  }
}
