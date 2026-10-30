package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.UpdateSubscriptionUpgrade;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class UpdateSubscriptionUpgradeRestValidator implements Consumer<UpdateSubscriptionUpgrade> {

  @Override
  public void accept(UpdateSubscriptionUpgrade updateSubscriptionUpgrade) {
    if (updateSubscriptionUpgrade == null) {
      throw new BadRequestException("Request body is mandatory.");
    }
    if (updateSubscriptionUpgrade.getStatus() == null) {
      throw new BadRequestException("status is mandatory.");
    }
  }
}
