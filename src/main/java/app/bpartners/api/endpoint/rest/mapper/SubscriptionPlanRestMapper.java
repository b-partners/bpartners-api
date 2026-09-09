package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.SubscriptionBillingType;
import app.bpartners.api.endpoint.rest.model.SubscriptionPlan;
import app.bpartners.api.endpoint.rest.model.SubscriptionPlanComparisonCellKind;
import app.bpartners.api.endpoint.rest.model.SubscriptionPlanComparisonEntry;
import app.bpartners.api.endpoint.rest.model.SubscriptionPlanDescription;
import app.bpartners.api.endpoint.rest.model.SubscriptionPlanFeatureItem;
import app.bpartners.api.endpoint.rest.model.SubscriptionPlanFeatureSection;
import app.bpartners.api.endpoint.rest.model.SubscriptionPlanFeatureStyle;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.model.subscription.SubscriptionProductComparisonEntry;
import app.bpartners.api.model.subscription.SubscriptionProductFeatureSection;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SubscriptionPlanRestMapper {

  public SubscriptionPlan toRest(SubscriptionProduct domain) {
    return new SubscriptionPlan()
        .id(domain.getId())
        .name(domain.getName())
        .description(domain.getDescription())
        .features(domain.getAllFeatures())
        .featureSections(featureSectionsToRest(domain.getFeatureSections()))
        .comparisonEntries(comparisonEntriesToRest(domain.getComparisonEntries()))
        .inheritedFromPlanName(domain.getInheritedFromPlanName())
        .billingType(billingTypeToRest(domain.getBillingType()))
        .priceInCentsWithVat(domain.getPriceInCentsWithVat())
        .priceInCentsWithoutVat(domain.getPriceInCentsWithoutVat())
        .vatPercent(domain.getVatPercent())
        .includedCreditsPerBillingPeriod(domain.includedCreditsPerBillingPeriodOrDefault())
        .creditCostPerAnalysis(domain.creditCostPerAnalysisOrDefault())
        .freeUsageThreshold(domain.getFreeUsageThreshold())
        .overageUnitPriceInCents(domain.getOverageUnitPriceInCents())
        .trialPeriodDays(domain.getTrialPeriodDays())
        .trialAnalysisGranted(domain.getTrialAnalysisGranted())
        .isMostChosen(domain.isMostChosen())
        .isDeprecated(domain.isDeprecated())
        .displayPosition(domain.getDisplayPosition())
        .annualDiscountPercent(domain.getAnnualDiscountPercent())
        .annualPriceInCentsWithVat(domain.getAnnualPriceInCentsWithVat())
        .annualPriceInCentsWithoutVat(domain.getAnnualPriceInCentsWithoutVat());
  }

  public SubscriptionPlanDescription toRestDescription(SubscriptionProduct domain) {
    return new SubscriptionPlanDescription()
        .id(domain.getId())
        .name(domain.getName())
        .description(domain.getDescription())
        .features(domain.getAllFeatures())
        .featureSections(featureSectionsToRest(domain.getFeatureSections()))
        .comparisonEntries(comparisonEntriesToRest(domain.getComparisonEntries()))
        .inheritedFromPlanName(domain.getInheritedFromPlanName())
        .billingType(billingTypeToRest(domain.getBillingType()))
        .priceInCentsWithVat(domain.getPriceInCentsWithVat())
        .priceInCentsWithoutVat(domain.getPriceInCentsWithoutVat())
        .isMostChosen(domain.isMostChosen())
        .isDeprecated(domain.isDeprecated())
        .displayPosition(domain.getDisplayPosition())
        .vatPercent(domain.getVatPercent());
  }

  private List<SubscriptionPlanFeatureSection> featureSectionsToRest(
      List<SubscriptionProductFeatureSection> domainSections) {
    if (domainSections == null) {
      return null;
    }
    return domainSections.stream().map(this::featureSectionToRest).toList();
  }

  private SubscriptionPlanFeatureSection featureSectionToRest(
      SubscriptionProductFeatureSection domainSection) {
    var items =
        domainSection.getItems() == null
            ? null
            : domainSection.getItems().stream()
                .map(
                    item ->
                        new SubscriptionPlanFeatureItem()
                            .text(item.getText())
                            .style(
                                item.getStyle() == null
                                    ? null
                                    : SubscriptionPlanFeatureStyle.valueOf(item.getStyle().name())))
                .toList();
    return new SubscriptionPlanFeatureSection().title(domainSection.getTitle()).items(items);
  }

  private List<SubscriptionPlanComparisonEntry> comparisonEntriesToRest(
      List<SubscriptionProductComparisonEntry> domainEntries) {
    if (domainEntries == null) {
      return null;
    }
    return domainEntries.stream().map(this::comparisonEntryToRest).toList();
  }

  private SubscriptionPlanComparisonEntry comparisonEntryToRest(
      SubscriptionProductComparisonEntry domainEntry) {
    return new SubscriptionPlanComparisonEntry()
        .sectionTitle(domainEntry.getSectionTitle())
        .label(domainEntry.getLabel())
        .kind(
            domainEntry.getKind() == null
                ? null
                : SubscriptionPlanComparisonCellKind.valueOf(domainEntry.getKind().name()))
        .text(domainEntry.getText());
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
