package app.bpartners.api.unit.mapper;

import static app.bpartners.api.model.subscription.SubscriptionBillingType.COMMITMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.api.endpoint.rest.mapper.SubscriptionPlanRestMapper;
import app.bpartners.api.endpoint.rest.model.SubscriptionBillingType;
import app.bpartners.api.endpoint.rest.model.SubscriptionPlanComparisonCellKind;
import app.bpartners.api.endpoint.rest.model.SubscriptionPlanFeatureStyle;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.model.subscription.SubscriptionProductComparisonCellKind;
import app.bpartners.api.model.subscription.SubscriptionProductComparisonEntry;
import app.bpartners.api.model.subscription.SubscriptionProductFeatureItem;
import app.bpartners.api.model.subscription.SubscriptionProductFeatureSection;
import app.bpartners.api.model.subscription.SubscriptionProductFeatureStyle;
import java.util.List;
import org.junit.jupiter.api.Test;

class SubscriptionPlanRestMapperTest {
  SubscriptionPlanRestMapper subject = new SubscriptionPlanRestMapper();

  private static SubscriptionProduct subscriptionProduct(
      app.bpartners.api.model.subscription.SubscriptionBillingType billingType) {
    return subscriptionProduct(billingType, false);
  }

  private static SubscriptionProduct subscriptionProduct(
      app.bpartners.api.model.subscription.SubscriptionBillingType billingType,
      boolean mostChosen) {
    return SubscriptionProduct.builder()
        .id("plan_id")
        .name("Premium")
        .description("Premium plan")
        .features(List.of("feature_1", "feature_2"))
        .billingType(billingType)
        .priceInCentsWithoutVat(4900L)
        .vatPercent(2000L)
        .mostChosen(mostChosen)
        .build();
  }

  @Test
  void to_rest_description_maps_all_fields() {
    var domain = subscriptionProduct(COMMITMENT);

    var actual = subject.toRestDescription(domain);

    assertEquals("plan_id", actual.getId());
    assertEquals("Premium", actual.getName());
    assertEquals("Premium plan", actual.getDescription());
    assertEquals(List.of("feature_1", "feature_2"), actual.getFeatures());
    assertEquals(SubscriptionBillingType.COMMITMENT, actual.getBillingType());
    assertEquals(4900L, actual.getPriceInCentsWithoutVat());
    assertEquals(5880L, actual.getPriceInCentsWithVat());
  }

  @Test
  void to_rest_description_maps_usage_based_billing_type() {
    var domain =
        subscriptionProduct(
            app.bpartners.api.model.subscription.SubscriptionBillingType.USAGE_BASED);

    var actual = subject.toRestDescription(domain);

    assertEquals(SubscriptionBillingType.USAGE_BASED, actual.getBillingType());
  }

  @Test
  void to_rest_description_maps_null_billing_type() {
    var domain = subscriptionProduct(null);

    var actual = subject.toRestDescription(domain);

    assertNull(actual.getBillingType());
  }

  @Test
  void to_rest_maps_prepaid_credit_fields() {
    var domain =
        subscriptionProduct(COMMITMENT).toBuilder()
            .includedCreditsPerBillingPeriod(10L)
            .creditCostPerAnalysis(2L)
            .build();

    var actual = subject.toRest(domain);

    assertEquals(10L, actual.getIncludedCreditsPerBillingPeriod());
    assertEquals(2L, actual.getCreditCostPerAnalysis());
  }

  @Test
  void to_rest_maps_prepaid_credit_fields_to_their_default() {
    var domain = subscriptionProduct(COMMITMENT);

    var actual = subject.toRest(domain);

    assertEquals(0L, actual.getIncludedCreditsPerBillingPeriod());
    assertEquals(1L, actual.getCreditCostPerAnalysis());
  }

  @Test
  void to_rest_maps_is_most_chosen() {
    var domain = subscriptionProduct(COMMITMENT, true);

    var actual = subject.toRest(domain);

    assertTrue(actual.getIsMostChosen());
  }

  @Test
  void to_rest_description_maps_is_most_chosen() {
    var domain = subscriptionProduct(COMMITMENT, true);

    var actual = subject.toRestDescription(domain);

    assertTrue(actual.getIsMostChosen());
  }

  @Test
  void to_rest_maps_is_most_chosen_false_by_default() {
    var domain = subscriptionProduct(COMMITMENT);

    assertFalse(subject.toRest(domain).getIsMostChosen());
    assertFalse(subject.toRestDescription(domain).getIsMostChosen());
  }

  @Test
  void to_rest_maps_is_deprecated() {
    var domain = subscriptionProduct(COMMITMENT).toBuilder().deprecated(true).build();

    var actual = subject.toRest(domain);

    assertTrue(actual.getIsDeprecated());
  }

  @Test
  void to_rest_description_maps_is_deprecated() {
    var domain = subscriptionProduct(COMMITMENT).toBuilder().deprecated(true).build();

    var actual = subject.toRestDescription(domain);

    assertTrue(actual.getIsDeprecated());
  }

  @Test
  void to_rest_maps_is_deprecated_false_by_default() {
    var domain = subscriptionProduct(COMMITMENT);

    assertFalse(subject.toRest(domain).getIsDeprecated());
    assertFalse(subject.toRestDescription(domain).getIsDeprecated());
  }

  @Test
  void to_rest_maps_display_position() {
    var domain = subscriptionProduct(COMMITMENT).toBuilder().displayPosition(3).build();

    var actual = subject.toRest(domain);

    assertEquals(3, actual.getDisplayPosition());
  }

  @Test
  void to_rest_description_maps_display_position() {
    var domain = subscriptionProduct(COMMITMENT).toBuilder().displayPosition(3).build();

    var actual = subject.toRestDescription(domain);

    assertEquals(3, actual.getDisplayPosition());
  }

  @Test
  void to_rest_maps_null_display_position_by_default() {
    var domain = subscriptionProduct(COMMITMENT);

    assertNull(subject.toRest(domain).getDisplayPosition());
    assertNull(subject.toRestDescription(domain).getDisplayPosition());
  }

  @Test
  void to_rest_maps_feature_sections_with_titles_styles_and_inline_bold() {
    var domain = subscriptionProduct(COMMITMENT).toBuilder().featureSections(sections()).build();

    var actual = subject.toRest(domain);

    var restSections = actual.getFeatureSections();
    assertEquals(2, restSections.size());
    assertEquals("Métrés inclus", restSections.get(0).getTitle());
    assertEquals(
        "Métrés 2D — surface, pente, périmètre", restSections.get(0).getItems().get(0).getText());
    assertEquals(
        SubscriptionPlanFeatureStyle.HIGHLIGHTED, restSections.get(0).getItems().get(0).getStyle());
    assertNull(restSections.get(1).getTitle());
    assertEquals(
        "**Communauté BIRDIA** — 1 chantier proposé / mois",
        restSections.get(1).getItems().get(0).getText());
    assertEquals(
        SubscriptionPlanFeatureStyle.EXCLUDED, restSections.get(1).getItems().get(1).getStyle());
  }

  @Test
  void features_are_derived_from_sections_with_bold_stripped_and_inherited_merged() {
    var parent =
        SubscriptionProduct.builder()
            .name("Essentiel")
            .featureSections(
                List.of(
                    SubscriptionProductFeatureSection.builder()
                        .items(
                            List.of(
                                SubscriptionProductFeatureItem.builder()
                                    .text("Support 7j/7 par email")
                                    .style(SubscriptionProductFeatureStyle.NORMAL)
                                    .build()))
                        .build()))
            .build();
    var domain =
        subscriptionProduct(COMMITMENT).toBuilder()
            .featureSections(sections())
            .includedSubscriptionProductFeatures(List.of(parent))
            .build();

    var actual = subject.toRest(domain);

    assertEquals("Essentiel", actual.getInheritedFromPlanName());
    assertEquals(
        List.of(
            "Métrés 2D — surface, pente, périmètre",
            "Communauté BIRDIA — 1 chantier proposé / mois",
            "Marque blanche / co-branding",
            "Support 7j/7 par email"),
        actual.getFeatures());
  }

  @Test
  void to_rest_maps_null_inherited_from_plan_name_by_default() {
    var domain = subscriptionProduct(COMMITMENT);

    assertNull(subject.toRest(domain).getInheritedFromPlanName());
    assertNull(subject.toRestDescription(domain).getInheritedFromPlanName());
  }

  @Test
  void to_rest_maps_comparison_entries_with_all_cell_kinds() {
    var domain =
        subscriptionProduct(COMMITMENT).toBuilder()
            .comparisonEntries(
                List.of(
                    SubscriptionProductComparisonEntry.builder()
                        .sectionTitle("Métrés — coeur BIRDIA")
                        .label("Surface rampant, pente, périmètre")
                        .kind(SubscriptionProductComparisonCellKind.INCLUDED)
                        .build(),
                    SubscriptionProductComparisonEntry.builder()
                        .sectionTitle("Intégration & monitoring")
                        .label("Accès API & webhooks")
                        .kind(SubscriptionProductComparisonCellKind.EXCLUDED)
                        .build(),
                    SubscriptionProductComparisonEntry.builder()
                        .sectionTitle("Support")
                        .label("Support")
                        .kind(SubscriptionProductComparisonCellKind.TEXT)
                        .text("Prioritaire")
                        .build()))
            .build();

    var entries = subject.toRest(domain).getComparisonEntries();

    assertEquals(3, entries.size());
    assertEquals("Métrés — coeur BIRDIA", entries.get(0).getSectionTitle());
    assertEquals("Surface rampant, pente, périmètre", entries.get(0).getLabel());
    assertEquals(SubscriptionPlanComparisonCellKind.INCLUDED, entries.get(0).getKind());
    assertNull(entries.get(0).getText());
    assertEquals(SubscriptionPlanComparisonCellKind.EXCLUDED, entries.get(1).getKind());
    assertNull(entries.get(1).getText());
    assertEquals(SubscriptionPlanComparisonCellKind.TEXT, entries.get(2).getKind());
    assertEquals("Prioritaire", entries.get(2).getText());
  }

  @Test
  void to_rest_maps_null_comparison_entries_by_default() {
    var domain = subscriptionProduct(COMMITMENT);

    assertNull(subject.toRest(domain).getComparisonEntries());
    assertNull(subject.toRestDescription(domain).getComparisonEntries());
  }

  private static List<SubscriptionProductFeatureSection> sections() {
    return List.of(
        SubscriptionProductFeatureSection.builder()
            .title("Métrés inclus")
            .items(
                List.of(
                    SubscriptionProductFeatureItem.builder()
                        .text("Métrés 2D — surface, pente, périmètre")
                        .style(SubscriptionProductFeatureStyle.HIGHLIGHTED)
                        .build()))
            .build(),
        SubscriptionProductFeatureSection.builder()
            .items(
                List.of(
                    SubscriptionProductFeatureItem.builder()
                        .text("**Communauté BIRDIA** — 1 chantier proposé / mois")
                        .style(SubscriptionProductFeatureStyle.NORMAL)
                        .build(),
                    SubscriptionProductFeatureItem.builder()
                        .text("Marque blanche / co-branding")
                        .style(SubscriptionProductFeatureStyle.EXCLUDED)
                        .build()))
            .build());
  }
}
