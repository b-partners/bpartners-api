package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.BillingInterval;
import app.bpartners.api.endpoint.rest.model.CreateSubscriptionInitiation;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class CreateSubscriptionInitiationRestValidator
    implements Consumer<CreateSubscriptionInitiation> {

  @Override
  public void accept(CreateSubscriptionInitiation createSubscriptionInitiation) {
    StringBuilder stringBuilder = new StringBuilder();
    if (createSubscriptionInitiation == null) {
      throw new IllegalArgumentException("createSubscriptionInitiation can not be null");
    } else {
      if (createSubscriptionInitiation.getSubscriptionType() == null
          && createSubscriptionInitiation.getSubscriptionPlanIdentifier() == null) {
        stringBuilder.append("planId or subscriptionType can not be both null. ");
      }
      if (createSubscriptionInitiation.getBillingInterval() == BillingInterval.YEARLY
          && createSubscriptionInitiation.getSubscriptionPlanIdentifier() == null) {
        stringBuilder.append(
            "subscriptionPlanIdentifier is required when billingInterval is YEARLY. ");
      }
      if (createSubscriptionInitiation.getRedirectionStatusUrls() == null) {
        stringBuilder.append("redirectionStatusUrls can not be null. ");
      } else {
        if (createSubscriptionInitiation.getRedirectionStatusUrls().getSuccessUrl() == null) {
          stringBuilder.append("redirectionStatusUrls.successUrl can not be null. ");
        }
        if (createSubscriptionInitiation.getRedirectionStatusUrls().getFailureUrl() == null) {
          stringBuilder.append("redirectionStatusUrls.failureUrl can not be null. ");
        }
      }
    }
    String exceptionMsg = stringBuilder.toString();
    if (!exceptionMsg.isEmpty()) {
      throw new IllegalArgumentException(exceptionMsg);
    }
  }
}
