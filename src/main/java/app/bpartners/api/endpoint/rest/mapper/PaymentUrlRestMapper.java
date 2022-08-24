package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.PaymentUrl;
import org.springframework.stereotype.Component;

@Component
public class PaymentUrlRestMapper {

  public PaymentUrl toRest(app.bpartners.api.model.PaymentUrl domain) {
    return new PaymentUrl()
        .redirectUrl(domain.getRedirectUrl())
        .successUrl(domain.getSuccessUrl())
        .failureUrl(domain.getFailureUrl());
  }
}
