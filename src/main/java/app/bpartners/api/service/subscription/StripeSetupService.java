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
  public static final String PAYMENT_METHOD_REPLACEMENT_METADATA_KEY = "payment_method_replacement";
  private static final String PAYMENT_METHOD_REPLACEMENT_METADATA_VALUE = "true";

  @SneakyThrows
  public Redirection setupCheckoutSession(
      String stripeCustomerIdentifier, RedirectionStatusUrls redirectionStatusUrls) {
    return setupCheckoutSession(stripeCustomerIdentifier, redirectionStatusUrls, false);
  }

  @SneakyThrows
  public Redirection setupReplacementCheckoutSession(
      String stripeCustomerIdentifier, RedirectionStatusUrls redirectionStatusUrls) {
    return setupCheckoutSession(stripeCustomerIdentifier, redirectionStatusUrls, true);
  }

  @SneakyThrows
  private Redirection setupCheckoutSession(
      String stripeCustomerIdentifier,
      RedirectionStatusUrls redirectionStatusUrls,
      boolean replaceExistingPaymentMethods) {
    var paramsBuilder =
        SessionCreateParams.builder()
            .setMode(SessionCreateParams.Mode.SETUP)
            .setCustomer(stripeCustomerIdentifier)
            .setSuccessUrl(redirectionStatusUrls.getSuccessUrl())
            .setCancelUrl(redirectionStatusUrls.getFailureUrl())
            .setCurrency(defaultCurrency());
    if (replaceExistingPaymentMethods) {
      paramsBuilder.putMetadata(
          PAYMENT_METHOD_REPLACEMENT_METADATA_KEY, PAYMENT_METHOD_REPLACEMENT_METADATA_VALUE);
    }

    var session = Session.create(paramsBuilder.build());

    return new Redirection()
        .redirectionUrl(session.getUrl())
        .redirectionStatusUrls(redirectionStatusUrls);
  }

  public static boolean isPaymentMethodReplacement(java.util.Map<String, String> sessionMetadata) {
    return sessionMetadata != null
        && PAYMENT_METHOD_REPLACEMENT_METADATA_VALUE.equals(
            sessionMetadata.get(PAYMENT_METHOD_REPLACEMENT_METADATA_KEY));
  }
}
