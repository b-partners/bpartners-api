package app.bpartners.api.unit.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import app.bpartners.api.model.subscription.SubscriptionProduct;
import org.junit.jupiter.api.Test;

class SubscriptionProductTest {

  @Test
  void price_ttc_is_derived_from_the_stored_ht_price_and_vat_as_integer_cents() {
    assertEquals(
        5880L,
        SubscriptionProduct.builder()
            .priceInCentsWithoutVat(4900L)
            .vatPercent(2000L)
            .build()
            .getPriceInCentsWithVat());
    assertEquals(
        840L,
        SubscriptionProduct.builder()
            .priceInCentsWithoutVat(700L)
            .vatPercent(2000L)
            .build()
            .getPriceInCentsWithVat());
    // Non-divisible HT is rounded half up to whole cents, never a floating amount.
    assertEquals(
        1010L,
        SubscriptionProduct.builder()
            .priceInCentsWithoutVat(842L)
            .vatPercent(2000L)
            .build()
            .getPriceInCentsWithVat());
  }

  @Test
  void price_ttc_is_null_when_ht_price_or_vat_is_unset() {
    assertNull(SubscriptionProduct.builder().vatPercent(2000L).build().getPriceInCentsWithVat());
    assertNull(
        SubscriptionProduct.builder()
            .priceInCentsWithoutVat(4900L)
            .build()
            .getPriceInCentsWithVat());
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
