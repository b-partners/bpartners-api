package app.bpartners.api.service.subscription;

import static app.bpartners.api.model.subscription.SubscriptionConsumptionType.ROOF_ANALYSIS;

import app.bpartners.api.model.subscription.SubscriptionConsumptionType;
import app.bpartners.api.repository.jpa.SubscriptionConsumptionLogJpaRepository;
import app.bpartners.api.repository.jpa.UserWhiteListedJpaRepository;
import org.springframework.stereotype.Component;

@Component
public class RoofAnalysisConsumptionFreeTrialValidator extends ConsumptionFreeTrialValidator {

  public RoofAnalysisConsumptionFreeTrialValidator(
      SubscriptionConsumptionLogJpaRepository consumptionLogJpaRepository,
      UserWhiteListedJpaRepository userWhiteListedJpaRepository) {
    super(consumptionLogJpaRepository, userWhiteListedJpaRepository);
  }

  @Override
  protected SubscriptionConsumptionType consumptionType() {
    return ROOF_ANALYSIS;
  }

  @Override
  protected String consumptionLabel() {
    return "Roof analysis";
  }
}
