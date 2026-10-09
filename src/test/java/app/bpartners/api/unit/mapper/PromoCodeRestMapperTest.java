package app.bpartners.api.unit.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import app.bpartners.api.endpoint.rest.mapper.PromoCodeRestMapper;
import app.bpartners.api.model.PromoCodeType;
import org.junit.jupiter.api.Test;

class PromoCodeRestMapperTest {
  PromoCodeRestMapper subject = new PromoCodeRestMapper();

  @Test
  void to_rest_maps_promo_code_fields() {
    var domain =
        app.bpartners.api.model.PromoCode.builder()
            .id("promo_id")
            .code("SUMMER2026")
            .type(PromoCodeType.AD)
            .label("Summer campaign")
            .build();

    var actual = subject.toRest(domain);

    assertEquals(
        new app.bpartners.api.endpoint.rest.model.PromoCode()
            .code("SUMMER2026")
            .type(app.bpartners.api.endpoint.rest.model.PromoCodeType.AD)
            .label("Summer campaign"),
        actual);
  }

  @Test
  void to_rest_maps_promo_code_stats() {
    var domain =
        app.bpartners.api.model.PromoCodeStats.builder()
            .promoCode(
                app.bpartners.api.model.PromoCode.builder()
                    .id("promo_id")
                    .code("SUMMER2026")
                    .type(PromoCodeType.AD)
                    .build())
            .registeredCount(42L)
            .build();

    var actual = subject.toRest(domain);

    assertEquals(
        new app.bpartners.api.endpoint.rest.model.PromoCodeStats()
            .promoCode(
                new app.bpartners.api.endpoint.rest.model.PromoCode()
                    .code("SUMMER2026")
                    .type(app.bpartners.api.endpoint.rest.model.PromoCodeType.AD))
            .registeredCount(42L),
        actual);
  }
}
