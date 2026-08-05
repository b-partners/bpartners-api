package app.bpartners.api.service.subscription;

import static app.bpartners.api.model.exception.ApiException.ExceptionType.SERVER_EXCEPTION;

import app.bpartners.api.endpoint.rest.model.Redirection;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.model.exception.ApiException;
import app.bpartners.api.model.exception.BadRequestException;
import com.stripe.model.billingportal.Session;
import com.stripe.param.billingportal.SessionCreateParams;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class StripePortalService {

  @SneakyThrows
  public Redirection initiateBillingPortalSession(
      String stripeCustomerIdentifier, RedirectionStatusUrls redirectionStatusUrls) {
    if (stripeCustomerIdentifier == null) {
      throw new ApiException(
          SERVER_EXCEPTION,
          "Unable to initiate billing portal session as user not associated to stripe customer");
    }
    validateRedirectionStatusUrls(redirectionStatusUrls);
    SessionCreateParams params =
        SessionCreateParams.builder()
            .setCustomer(stripeCustomerIdentifier)
            .setReturnUrl(redirectionStatusUrls.getSuccessUrl())
            .build();
    Session session = Session.create(params);

    return new Redirection()
        .redirectionUrl(session.getUrl())
        .redirectionStatusUrls(redirectionStatusUrls);
  }

  private void validateRedirectionStatusUrls(RedirectionStatusUrls redirectionStatusUrls) {
    StringBuilder exceptionMessageBuilder = new StringBuilder();
    if (redirectionStatusUrls == null) {
      exceptionMessageBuilder.append("RedirectionStatusUrls is mandatory");
    } else {
      if (redirectionStatusUrls.getSuccessUrl() == null) {
        exceptionMessageBuilder.append("RedirectionStatusUrls.successUrl is mandatory. ");
      }
      if (redirectionStatusUrls.getFailureUrl() == null) {
        exceptionMessageBuilder.append("RedirectionStatusUrls.failureUrl is mandatory.");
      }
    }
    var exceptionMessage = exceptionMessageBuilder.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }
}
