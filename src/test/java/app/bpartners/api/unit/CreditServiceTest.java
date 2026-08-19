package app.bpartners.api.unit;

import static app.bpartners.api.model.credit.CreditCode.ANALYSES_10;
import static app.bpartners.api.model.subscription.SubscriptionBillingType.USAGE_BASED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.model.User;
import app.bpartners.api.model.UserSubscriptionProduct;
import app.bpartners.api.model.credit.CreditPack;
import app.bpartners.api.model.credit.CreditPurchaseType;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.repository.jpa.CreditPackRepository;
import app.bpartners.api.repository.jpa.SubscriptionProductRepository;
import app.bpartners.api.repository.jpa.UserSubscriptionProductJpaRepository;
import app.bpartners.api.service.credit.CreditService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CreditServiceTest {
  CreditPackRepository creditPackRepository = mock(CreditPackRepository.class);
  UserSubscriptionProductJpaRepository userSubscriptionProductJpaRepository =
      mock(UserSubscriptionProductJpaRepository.class);
  SubscriptionProductRepository subscriptionProductRepository =
      mock(SubscriptionProductRepository.class);

  CreditService subject =
      new CreditService(
          creditPackRepository,
          userSubscriptionProductJpaRepository,
          subscriptionProductRepository);

  User user = User.builder().id("user_id").build();

  @Test
  void resolve_unit_price_from_active_plan() {
    when(userSubscriptionProductJpaRepository.findAllByUserIdAndSubscriptionEndDatetimeIsNull(
            "user_id"))
        .thenReturn(
            List.of(
                UserSubscriptionProduct.builder()
                    .subscriptionProduct(
                        SubscriptionProduct.builder()
                            .creditUnitPriceInCentsWithoutVat(400L)
                            .vatPercent(2000L)
                            .build())
                    .build()));

    var actual = subject.resolveCreditUnitPrice(user);

    assertEquals(400L, actual.inCentsWithoutVat());
    assertEquals(480L, actual.inCentsWithVat());
    assertEquals(2000L, actual.vatPercent());
  }

  @Test
  void resolve_unit_price_falls_back_to_usage_based_plan_when_no_active_plan() {
    when(userSubscriptionProductJpaRepository.findAllByUserIdAndSubscriptionEndDatetimeIsNull(
            "user_id"))
        .thenReturn(List.of());
    when(subscriptionProductRepository.findFirstByBillingType(USAGE_BASED))
        .thenReturn(
            Optional.of(
                SubscriptionProduct.builder()
                    .billingType(USAGE_BASED)
                    .creditUnitPriceInCentsWithoutVat(1000L)
                    .vatPercent(2000L)
                    .build()));

    var actual = subject.resolveCreditUnitPrice(user);

    assertEquals(1000L, actual.inCentsWithoutVat());
    assertEquals(1200L, actual.inCentsWithVat());
  }

  @Test
  void resolve_unit_price_falls_back_to_default_usage_baseline_when_no_plan_at_all() {
    when(userSubscriptionProductJpaRepository.findAllByUserIdAndSubscriptionEndDatetimeIsNull(
            "user_id"))
        .thenReturn(List.of());
    when(subscriptionProductRepository.findFirstByBillingType(USAGE_BASED))
        .thenReturn(Optional.empty());

    var actual = subject.resolveCreditUnitPrice(user);

    assertEquals(1000L, actual.inCentsWithoutVat());
    assertEquals(1200L, actual.inCentsWithVat());
    assertEquals(2000L, actual.vatPercent());
  }

  @Test
  void get_credit_pack_by_id() {
    var pack =
        CreditPack.builder()
            .id("pack_10")
            .code(ANALYSES_10)
            .creditPurchaseType(CreditPurchaseType.PACK)
            .build();
    when(creditPackRepository.findById("pack_10")).thenReturn(Optional.of(pack));

    assertEquals(pack, subject.getCreditPack("pack_10"));
  }

  @Test
  void get_credit_pack_by_unknown_id_throws_not_found() {
    when(creditPackRepository.findById("unknown")).thenReturn(Optional.empty());

    assertThrows(NotFoundException.class, () -> subject.getCreditPack("unknown"));
  }
}
