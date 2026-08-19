package app.bpartners.api.unit.mapper;

import static app.bpartners.api.model.credit.CreditCode.ANALYSES_10;
import static app.bpartners.api.model.credit.CreditCode.PACK_CUSTOM;
import static app.bpartners.api.model.credit.CreditPurchaseType.CUSTOM;
import static app.bpartners.api.model.credit.CreditPurchaseType.PACK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.api.endpoint.rest.mapper.CreditPackRestMapper;
import app.bpartners.api.endpoint.rest.model.CreditPurchaseType;
import app.bpartners.api.model.credit.CreditPack;
import app.bpartners.api.model.credit.CreditUnitPrice;
import org.junit.jupiter.api.Test;

class CreditPackRestMapperTest {
  CreditPackRestMapper subject = new CreditPackRestMapper();
  CreditUnitPrice essentialUnitPrice = new CreditUnitPrice(500L, 2000L);

  private static CreditPack.CreditPackBuilder packBuilder() {
    return CreditPack.builder()
        .id("pack_id")
        .code(ANALYSES_10)
        .description("10 analyses de toiture")
        .creditPurchaseType(PACK)
        .credits(10L)
        .validityDays(365)
        .displayPosition(2);
  }

  @Test
  void to_rest_maps_all_fields_and_computes_total_price() {
    var actual = subject.toRest(packBuilder().mostChosen(true).build(), essentialUnitPrice);

    assertEquals("pack_id", actual.getId());
    assertEquals(ANALYSES_10.toString(), actual.getCode());
    assertEquals("10 analyses de toiture", actual.getDescription());
    assertEquals(CreditPurchaseType.PACK, actual.getCreditPurchaseType());
    assertEquals(10L, actual.getCredits());
    assertEquals(500L, actual.getCreditUnitPriceInCentsWithoutVat());
    assertEquals(600L, actual.getCreditUnitPriceInCentsWithVat());
    assertEquals(5000L, actual.getPriceInCentsWithoutVat());
    assertEquals(6000L, actual.getPriceInCentsWithVat());
    assertEquals(2000L, actual.getVatPercent());
    assertEquals(365, actual.getValidityDays());
    assertEquals(2, actual.getDisplayPosition());
    assertTrue(actual.getIsMostChosen());
  }

  @Test
  void to_rest_prices_the_same_pack_lower_on_a_better_plan() {
    var proUnitPrice = new CreditUnitPrice(400L, 2000L);

    var actual = subject.toRest(packBuilder().build(), proUnitPrice);

    assertEquals(4000L, actual.getPriceInCentsWithoutVat());
    assertEquals(4800L, actual.getPriceInCentsWithVat());
  }

  @Test
  void to_rest_maps_custom_pack_with_unit_price_but_no_total() {
    var actual =
        subject.toRest(
            packBuilder().code(PACK_CUSTOM).creditPurchaseType(CUSTOM).credits(null).build(),
            essentialUnitPrice);

    assertEquals(CreditPurchaseType.CUSTOM, actual.getCreditPurchaseType());
    assertNull(actual.getCredits());
    assertEquals(500L, actual.getCreditUnitPriceInCentsWithoutVat());
    assertEquals(600L, actual.getCreditUnitPriceInCentsWithVat());
    assertNull(actual.getPriceInCentsWithoutVat());
    assertNull(actual.getPriceInCentsWithVat());
  }

  @Test
  void to_rest_maps_null_purchase_type() {
    var actual = subject.toRest(packBuilder().creditPurchaseType(null).build(), essentialUnitPrice);

    assertNull(actual.getCreditPurchaseType());
  }

  @Test
  void to_rest_maps_flags_false_by_default() {
    var actual = subject.toRest(packBuilder().build(), essentialUnitPrice);

    assertFalse(actual.getIsMostChosen());
    assertFalse(actual.getIsDeprecated());
  }
}
