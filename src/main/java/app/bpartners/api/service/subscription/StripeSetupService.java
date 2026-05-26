package app.bpartners.api.service.subscription;

import static app.bpartners.api.payment.StripeConf.defaultCurrency;

import app.bpartners.api.endpoint.rest.model.Redirection;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

@Service
public class StripeSetupService {

  @SneakyThrows
  public Redirection setupCheckoutSession(
      String stripeCustomerIdentifier, RedirectionStatusUrls redirectionStatusUrls) {
    SessionCreateParams params =
        SessionCreateParams.builder()
            .setMode(SessionCreateParams.Mode.SETUP)
            .setCustomer(stripeCustomerIdentifier)
            .setSuccessUrl(redirectionStatusUrls.getSuccessUrl())
            .setCancelUrl(redirectionStatusUrls.getFailureUrl())
            .setCurrency(defaultCurrency())
            .build();

    var session = Session.create(params);

    return new Redirection()
        .redirectionUrl(session.getUrl())
        .redirectionStatusUrls(redirectionStatusUrls);
  }
}
