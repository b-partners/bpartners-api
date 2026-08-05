package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.SubscriptionBillingType;
import app.bpartners.api.endpoint.rest.model.SubscriptionPlan;
import app.bpartners.api.endpoint.rest.model.SubscriptionPlanDescription;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionPlanRestMapper {

  public SubscriptionPlan toRest(SubscriptionProduct domain) {
    return new SubscriptionPlan()
        .id(domain.getId())
        .name(domain.getName())
        .description(domain.getDescription())
        .features(domain.getFeatures())
        .billingType(billingTypeToRest(domain.getBillingType()))
        .priceInCentsWithVat(domain.getPriceInCentsWithVat())
        .priceInCentsWithoutVat(domain.getPriceInCentsWithoutVat())
        .vatPercent(domain.getVatPercent())
        .freeUsageThreshold(domain.getFreeUsageThreshold())
        .overageUnitPriceInCents(domain.getOverageUnitPriceInCents())
        .trialPeriodDays(domain.getTrialPeriodDays())
        .isMostChosen(domain.isMostChosen())
        .isDeprecated(domain.isDeprecated())
        .displayPosition(domain.getDisplayPosition())
        .annualDiscountPercent(domain.getAnnualDiscountPercent());
  }

  public SubscriptionPlanDescription toRestDescription(SubscriptionProduct domain) {
    return new SubscriptionPlanDescription()
        .id(domain.getId())
        .name(domain.getName())
        .description(domain.getDescription())
        .features(domain.getFeatures())
        .billingType(billingTypeToRest(domain.getBillingType()))
        .priceInCentsWithVat(domain.getPriceInCentsWithVat())
        .priceInCentsWithoutVat(domain.getPriceInCentsWithoutVat())
        .isMostChosen(domain.isMostChosen())
        .isDeprecated(domain.isDeprecated())
        .displayPosition(domain.getDisplayPosition())
        .vatPercent(domain.getVatPercent());
  }

  private SubscriptionBillingType billingTypeToRest(
      app.bpartners.api.model.subscription.SubscriptionBillingType domainBillingType) {
    if (domainBillingType == null) {
      return null;
    }
    return switch (domainBillingType) {
      case COMMITMENT -> SubscriptionBillingType.COMMITMENT;
      case USAGE_BASED -> SubscriptionBillingType.USAGE_BASED;
    };
  }
}
