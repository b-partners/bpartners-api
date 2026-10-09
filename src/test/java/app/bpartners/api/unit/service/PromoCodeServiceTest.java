package app.bpartners.api.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.model.PromoCode;
import app.bpartners.api.model.PromoCodeType;
import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.repository.PromoCodeJpaRepository;
import app.bpartners.api.repository.UserRepository;
import app.bpartners.api.service.user.PromoCodeService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PromoCodeServiceTest {
  PromoCodeJpaRepository promoCodeJpaRepositoryMock = mock(PromoCodeJpaRepository.class);
  UserRepository userRepositoryMock = mock(UserRepository.class);
  PromoCodeService subject = new PromoCodeService(promoCodeJpaRepositoryMock, userRepositoryMock);

  @Test
  void resolve_id_returns_null_for_blank_code() {
    assertNull(subject.resolveId(null));
    assertNull(subject.resolveId(""));
    assertNull(subject.resolveId("  "));
  }

  @Test
  void resolve_id_returns_null_for_an_unknown_code() {
    when(promoCodeJpaRepositoryMock.findByCodeAndDeprecatedFalse("UNKNOWN"))
        .thenReturn(Optional.empty());

    assertNull(subject.resolveId("UNKNOWN"));
  }

  @Test
  void resolve_id_returns_the_promo_code_id_for_an_active_code() {
    when(promoCodeJpaRepositoryMock.findByCodeAndDeprecatedFalse("SUMMER2026"))
        .thenReturn(
            Optional.of(
                PromoCode.builder()
                    .id("promo_id")
                    .code("SUMMER2026")
                    .type(PromoCodeType.AD)
                    .build()));

    assertEquals("promo_id", subject.resolveId("SUMMER2026"));
  }

  @Test
  void get_stats_computes_registered_count_per_code() {
    var promoCode =
        PromoCode.builder().id("promo_id").code("SUMMER2026").type(PromoCodeType.AD).build();
    when(promoCodeJpaRepositoryMock.findAll()).thenReturn(List.of(promoCode));
    when(userRepositoryMock.countByPromoCodeId("promo_id")).thenReturn(3L);

    var actual = subject.getStats();

    assertEquals(1, actual.size());
    assertEquals(promoCode, actual.getFirst().getPromoCode());
    assertEquals(3L, actual.getFirst().getRegisteredCount());
  }

  @Test
  void get_users_by_code_delegates_to_the_user_repository() {
    var promoCode =
        PromoCode.builder().id("promo_id").code("SUMMER2026").type(PromoCodeType.AD).build();
    var user = User.builder().id("user_id").build();
    when(promoCodeJpaRepositoryMock.findByCode("SUMMER2026")).thenReturn(Optional.of(promoCode));
    when(userRepositoryMock.findAllByPromoCodeId("promo_id")).thenReturn(List.of(user));

    assertEquals(List.of(user), subject.getUsersByCode("SUMMER2026"));
  }

  @Test
  void get_by_code_rejects_an_unknown_code() {
    when(promoCodeJpaRepositoryMock.findByCode("UNKNOWN")).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> subject.getByCode("UNKNOWN"));
  }
}
