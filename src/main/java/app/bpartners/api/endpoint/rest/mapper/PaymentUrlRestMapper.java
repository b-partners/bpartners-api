package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.PaymentRedirection;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import org.springframework.stereotype.Component;

@Component
public class PaymentUrlRestMapper {

  public PaymentRedirection toRest(app.bpartners.api.model.PaymentRedirection domain) {
    RedirectionStatusUrls statusUrls = new RedirectionStatusUrls();
    statusUrls.setSuccessUrl(domain.getSuccessUrl());
    statusUrls.setFailureUrl(domain.getFailureUrl());
    return new PaymentRedirection()
        .redirectionUrl(domain.getRedirectUrl())
        .redirectionStatusUrls(statusUrls);
  }
}
