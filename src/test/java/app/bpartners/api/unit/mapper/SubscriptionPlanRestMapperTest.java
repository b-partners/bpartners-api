package app.bpartners.api.unit.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import app.bpartners.api.endpoint.rest.mapper.SubscriptionPlanRestMapper;
import app.bpartners.api.endpoint.rest.model.SubscriptionBillingType;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import java.util.List;
import org.junit.jupiter.api.Test;

class SubscriptionPlanRestMapperTest {
  SubscriptionPlanRestMapper subject = new SubscriptionPlanRestMapper();

  private static SubscriptionProduct subscriptionProduct(
      app.bpartners.api.model.subscription.SubscriptionBillingType billingType) {
    return SubscriptionProduct.builder()
        .id("plan_id")
        .name("Premium")
        .description("Premium plan")
        .features(List.of("feature_1", "feature_2"))
        .billingType(billingType)
        .priceInCentsWithoutVat(4900L)
        .vatPercent(2000L)
        .build();
  }

  @Test
  void to_rest_description_maps_all_fields() {
    var domain =
        subscriptionProduct(
            app.bpartners.api.model.subscription.SubscriptionBillingType.COMMITMENT);

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
}
