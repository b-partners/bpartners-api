package app.bpartners.api.unit.mapper;

import static app.bpartners.api.model.credit.CreditCode.ANALYSES_10;
import static app.bpartners.api.model.credit.CreditPurchaseType.PACK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import app.bpartners.api.endpoint.rest.mapper.CreditPackRestMapper;
import app.bpartners.api.endpoint.rest.mapper.CreditPurchaseRestMapper;
import app.bpartners.api.model.credit.CreditPack;
import app.bpartners.api.model.credit.CreditPurchase;
import org.junit.jupiter.api.Test;

class CreditPurchaseRestMapperTest {
  CreditPurchaseRestMapper subject = new CreditPurchaseRestMapper(new CreditPackRestMapper());

  @Test
  void map_an_empty_purchase_without_failing() {
    var actual = subject.toRest(CreditPurchase.builder().id("purchase_1").build());

    assertEquals("purchase_1", actual.getId());
    assertNull(actual.getType());
    assertNull(actual.getStatus());
    assertNull(actual.getOrigin());
    assertNull(actual.getPackPurchase());
    assertNull(actual.getCustomPurchase());
    assertNull(actual.getRedirection());
  }

  @Test
  void map_a_pack_purchase_whose_pack_is_gone() {
    var actual =
        subject.toRest(CreditPurchase.builder().id("purchase_1").type(PACK).quantity(2).build());

    assertNull(actual.getPackPurchase());
    assertNull(actual.getCustomPurchase());
  }

  @Test
  void map_a_pack_purchase_without_unit_price_applied() {
    var actual =
        subject.toRest(
            CreditPurchase.builder()
                .id("purchase_1")
                .type(PACK)
                .creditPack(
                    CreditPack.builder().id("pack_10").code(ANALYSES_10).credits(10L).build())
                .quantity(1)
                .build());

    var pack = actual.getPackPurchase().getCreditPack();
    assertNull(pack.getCreditPurchaseType());
    assertEquals(0L, pack.getCreditUnitPriceInCentsWithoutVat());
    assertEquals(0L, pack.getCreditUnitPriceInCentsWithVat());
    assertEquals(0L, pack.getPriceInCentsWithoutVat());
    assertEquals(0L, pack.getVatPercent());
  }

  @Test
  void map_a_redirection_without_status_urls() {
    var actual =
        subject.toRest(
            CreditPurchase.builder()
                .id("purchase_1")
                .redirectionUrl("https://pay.stripe.com/session")
                .build());

    assertEquals("https://pay.stripe.com/session", actual.getRedirection().getRedirectionUrl());
    assertNull(actual.getRedirection().getRedirectionStatusUrls());
  }

  @Test
  void map_status_urls_without_redirection_url() {
    var actual =
        subject.toRest(
            CreditPurchase.builder()
                .id("purchase_1")
                .redirectionSuccessUrl("https://birdia.fr/success")
                .build());

    assertNull(actual.getRedirection().getRedirectionUrl());
    assertEquals(
        "https://birdia.fr/success",
        actual.getRedirection().getRedirectionStatusUrls().getSuccessUrl());
    assertNull(actual.getRedirection().getRedirectionStatusUrls().getFailureUrl());
  }

  @Test
  void map_a_failure_url_only_redirection() {
    var actual =
        subject.toRest(
            CreditPurchase.builder()
                .id("purchase_1")
                .redirectionFailureUrl("https://birdia.fr/failure")
                .build());

    assertEquals(
        "https://birdia.fr/failure",
        actual.getRedirection().getRedirectionStatusUrls().getFailureUrl());
  }

  @Test
  void map_a_null_status_to_no_domain_status() {
    assertNull(subject.toDomainStatus(null));
  }
}
