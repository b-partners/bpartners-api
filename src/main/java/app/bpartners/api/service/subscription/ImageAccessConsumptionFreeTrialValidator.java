package app.bpartners.api.service.subscription;

import static app.bpartners.api.model.subscription.SubscriptionConsumptionType.IMAGE_ACCESS;

import app.bpartners.api.model.subscription.SubscriptionConsumptionType;
import app.bpartners.api.repository.jpa.SubscriptionConsumptionLogJpaRepository;
import app.bpartners.api.repository.jpa.UserWhiteListedJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class ImageAccessConsumptionFreeTrialValidator extends ConsumptionFreeTrialValidator {

  public ImageAccessConsumptionFreeTrialValidator(
      SubscriptionConsumptionLogJpaRepository consumptionLogJpaRepository,
      UserWhiteListedJpaRepository userWhiteListedJpaRepository) {
    super(consumptionLogJpaRepository, userWhiteListedJpaRepository);
  }

  @Override
  protected SubscriptionConsumptionType consumptionType() {
    return IMAGE_ACCESS;
  }

  @Override
  protected String consumptionLabel() {
    return "Image access";
  }
}
