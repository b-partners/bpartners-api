package app.bpartners.api.unit.mapper;

import static app.bpartners.api.model.subscription.SubscriptionBillingType.COMMITMENT;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.api.endpoint.rest.mapper.SubscriptionPlanRestMapper;
import app.bpartners.api.endpoint.rest.model.SubscriptionBillingType;
import app.bpartners.api.model.subscription.SubscriptionProduct;
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
}
