package app.bpartners.api.unit.model;

import static app.bpartners.api.model.credit.CreditPurchaseType.CUSTOM;
import static app.bpartners.api.model.credit.CreditPurchaseType.PACK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import app.bpartners.api.model.credit.CreditPack;
import app.bpartners.api.model.credit.CreditPurchase;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class CreditPurchaseTest {

  @Test
  void pack_purchase_is_labelled_after_the_bought_pack() {
    var actual =
        CreditPurchase.builder()
            .type(PACK)
            .creditPack(CreditPack.builder().description("10 analyses de toiture").build())
            .credits(20L)
            .build();

    assertEquals("10 analyses de toiture", actual.paymentLabel());
  }

  @Test
  void custom_purchase_is_labelled_after_the_bought_credits() {
    var actual = CreditPurchase.builder().type(CUSTOM).credits(7L).build();

    assertEquals("7 crédits d'analyse", actual.paymentLabel());
  }

  @Test
  void pack_purchase_without_pack_description_falls_back_on_the_credits() {
    var actual =
        CreditPurchase.builder()
            .type(PACK)
            .creditPack(CreditPack.builder().id("pack_10").build())
            .credits(20L)
            .build();

    assertEquals("20 crédits d'analyse", actual.paymentLabel());
  }

  @Test
  void unit_price_applied_defaults_to_zero_when_not_priced_yet() {
    var actual = CreditPurchase.builder().type(CUSTOM).credits(7L).build();

    assertEquals(0L, actual.unitPriceApplied().inCentsWithoutVat());
    assertEquals(0L, actual.unitPriceApplied().vatPercent());
  }

  @Test
  void datetimes_are_exposed_at_the_precision_the_database_keeps() {
    var stampedWithNanos = Instant.parse("2026-08-01T00:00:00.123456789Z");

    var actual =
        CreditPurchase.builder()
            .creationDatetime(stampedWithNanos)
            .completionDatetime(stampedWithNanos)
            .build();

    assertEquals(Instant.parse("2026-08-01T00:00:00.123Z"), actual.getCreationDatetime());
    assertEquals(Instant.parse("2026-08-01T00:00:00.123Z"), actual.getCompletionDatetime());
  }

  @Test
  void missing_datetimes_stay_null() {
    var actual = CreditPurchase.builder().build();

    assertNull(actual.getCreationDatetime());
    assertNull(actual.getCompletionDatetime());
  }
}
