package app.bpartners.api.unit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.controller.PromoCodeController;
import app.bpartners.api.endpoint.rest.mapper.PromoCodeRestMapper;
import app.bpartners.api.endpoint.rest.mapper.UserRestMapper;
import app.bpartners.api.model.PromoCode;
import app.bpartners.api.model.PromoCodeStats;
import app.bpartners.api.model.PromoCodeType;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.service.user.PromoCodeService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PromoCodeControllerTest {
  PromoCodeService promoCodeServiceMock = mock(PromoCodeService.class);
  PromoCodeRestMapper promoCodeMapper = new PromoCodeRestMapper();
  UserRestMapper userMapperMock = mock(UserRestMapper.class);

  PromoCodeController subject =
      new PromoCodeController(promoCodeServiceMock, promoCodeMapper, userMapperMock);

  @Test
  void check_an_active_promo_code() {
    when(promoCodeServiceMock.findActiveByCode("SUMMER2026"))
        .thenReturn(
            Optional.of(
                PromoCode.builder()
                    .id("promo_id")
                    .code("SUMMER2026")
                    .type(PromoCodeType.AD)
                    .build()));

    var actual = subject.checkPromoCode("SUMMER2026");

    assertEquals(
        new app.bpartners.api.endpoint.rest.model.PromoCode()
            .code("SUMMER2026")
            .type(app.bpartners.api.endpoint.rest.model.PromoCodeType.AD),
        actual);
  }

  @Test
  void check_an_unknown_promo_code_is_rejected() {
    when(promoCodeServiceMock.findActiveByCode("UNKNOWN")).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> subject.checkPromoCode("UNKNOWN"));
  }

  @Test
  void get_promo_codes_stats() {
    when(promoCodeServiceMock.getStats())
        .thenReturn(
            List.of(
                PromoCodeStats.builder()
                    .promoCode(
                        PromoCode.builder()
                            .id("promo_id")
                            .code("SUMMER2026")
                            .type(PromoCodeType.AD)
                            .build())
                    .registeredCount(3L)
                    .build()));

    var actual = subject.getPromoCodes();

    assertEquals(
        List.of(
            new app.bpartners.api.endpoint.rest.model.PromoCodeStats()
                .promoCode(
                    new app.bpartners.api.endpoint.rest.model.PromoCode()
                        .code("SUMMER2026")
                        .type(app.bpartners.api.endpoint.rest.model.PromoCodeType.AD))
                .registeredCount(3L)),
        actual);
  }

  @Test
  void get_users_registered_under_a_promo_code() {
    var user = User.builder().id("user_id").build();
    var restUser = new app.bpartners.api.endpoint.rest.model.User().id("user_id");
    when(promoCodeServiceMock.getUsersByCode("SUMMER2026")).thenReturn(List.of(user));
    when(userMapperMock.toRest(user)).thenReturn(restUser);

    var actual = subject.getPromoCodeUsers("SUMMER2026");

    assertEquals(List.of(restUser), actual);
  }
}
