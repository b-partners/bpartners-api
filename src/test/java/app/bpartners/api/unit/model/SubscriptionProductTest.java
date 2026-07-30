package app.bpartners.api.unit.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.api.model.subscription.SubscriptionProduct;
import org.junit.jupiter.api.Test;

class SubscriptionProductTest {

  @Test
  void fixed_price_ht_is_derived_from_the_stored_ttc_price_and_vat_as_integer_cents() {
    assertEquals(
        4900L, SubscriptionProduct.builder().priceInCents(5880L).build().fixedPriceHtInCents(2000));
    assertEquals(
        700L, SubscriptionProduct.builder().priceInCents(840L).build().fixedPriceHtInCents(2000));
    // Non-divisible TTC is rounded half up to whole cents, never a floating amount.
    assertEquals(
        842L, SubscriptionProduct.builder().priceInCents(1010L).build().fixedPriceHtInCents(2000));
  }

  @Test
  void free_usage_threshold_falls_back_to_the_historical_default_when_unset() {
    assertEquals(
        SubscriptionProduct.DEFAULT_FREE_USAGE_THRESHOLD,
        SubscriptionProduct.builder().build().freeUsageThresholdOrDefault());
    assertEquals(
        5L,
        SubscriptionProduct.builder().freeUsageThreshold(5L).build().freeUsageThresholdOrDefault());
  }

  @Test
  void overage_unit_price_falls_back_to_the_historical_default_when_unset() {
    assertEquals(
        SubscriptionProduct.DEFAULT_OVERAGE_UNIT_PRICE_IN_CENTS,
        SubscriptionProduct.builder().build().overageUnitPriceInCentsOrDefault());
    assertEquals(
        300L,
        SubscriptionProduct.builder()
            .overageUnitPriceInCents(300L)
            .build()
            .overageUnitPriceInCentsOrDefault());
  }
}
