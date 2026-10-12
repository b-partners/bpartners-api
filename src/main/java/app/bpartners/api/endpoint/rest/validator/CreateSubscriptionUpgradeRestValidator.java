package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.CreateSubscriptionUpgrade;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class CreateSubscriptionUpgradeRestValidator implements Consumer<CreateSubscriptionUpgrade> {

  @Override
  public void accept(CreateSubscriptionUpgrade createSubscriptionUpgrade) {
    if (createSubscriptionUpgrade == null) {
      throw new BadRequestException("Request body is mandatory.");
    }
    var subscriptionPlanIdentifier = createSubscriptionUpgrade.getSubscriptionPlanIdentifier();
    if (subscriptionPlanIdentifier == null || subscriptionPlanIdentifier.isBlank()) {
      throw new BadRequestException("subscriptionPlanIdentifier is mandatory.");
    }
  }
}
