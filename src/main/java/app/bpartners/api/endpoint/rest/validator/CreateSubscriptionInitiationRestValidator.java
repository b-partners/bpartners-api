package app.bpartners.api.endpoint.rest.validator;

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
      if (createSubscriptionInitiation.getSubscriptionType() == null) {
        stringBuilder.append("subscriptionType can not be null. ");
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
