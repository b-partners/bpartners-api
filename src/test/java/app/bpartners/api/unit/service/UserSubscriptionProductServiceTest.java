package app.bpartners.api.unit.service;

import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.api.model.UserSubscriptionProduct;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.repository.jpa.SubscriptionProductRepository;
import app.bpartners.api.repository.jpa.UserSubscriptionProductJpaRepository;
import app.bpartners.api.service.credit.CreditGrantService;
import app.bpartners.api.service.subscription.UserSubscriptionProductService;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class UserSubscriptionProductServiceTest {
  UserSubscriptionProductJpaRepository userSubscriptionProductJpaRepository = mock();
  SubscriptionProductRepository subscriptionProductRepository = mock();
  CreditGrantService creditGrantService = mock();
  UserSubscriptionProductService subject =
      new UserSubscriptionProductService(
          userSubscriptionProductJpaRepository, subscriptionProductRepository, creditGrantService);

  @BeforeEach
  void setUp() {
    when(userSubscriptionProductJpaRepository.save(any())).thenAnswer(i -> i.getArgument(0));
  }

  @Test
  void creates_subscribed_product_with_null_end_datetime_when_no_active_one() {
    var userId = randomUUID().toString();
    var subscribedPlanId = "usage_based_plan_id";
    var subscribedPlan = SubscriptionProduct.builder().id(subscribedPlanId).build();
    when(subscriptionProductRepository.findById(subscribedPlanId))
        .thenReturn(Optional.of(subscribedPlan));
    when(userSubscriptionProductJpaRepository.existsByUserIdAndSubscriptionEndDatetimeIsNull(
            userId))
        .thenReturn(false);

    var actual = subject.ensureActiveSubscriptionProduct(userId, subscribedPlanId);

    var captor = ArgumentCaptor.forClass(UserSubscriptionProduct.class);
    verify(userSubscriptionProductJpaRepository).save(captor.capture());
    var saved = captor.getValue();
    assertTrue(actual.isPresent());
    assertEquals(userId, saved.getUserId());
    assertEquals(subscribedPlanId, saved.getSubscriptionProduct().getId());
    assertNull(saved.getSubscriptionEndDatetime());
    assertNotNull(saved.getSubscriptionStartDatetime());
    assertNotNull(saved.getCreationDatetime());
    verify(creditGrantService).grantIncludedCredits(userId, subscribedPlan);
  }

  @Test
  void is_idempotent_and_does_not_create_when_active_one_already_exists() {
    var userId = randomUUID().toString();
    when(userSubscriptionProductJpaRepository.existsByUserIdAndSubscriptionEndDatetimeIsNull(
            userId))
        .thenReturn(true);

    var actual = subject.ensureActiveSubscriptionProduct(userId, "any_plan_id");

    assertTrue(actual.isEmpty());
    verify(userSubscriptionProductJpaRepository, never()).save(any());
    verify(creditGrantService, never()).grantIncludedCredits(any(), any());
  }

  @Test
  void throws_when_plan_id_null() {
    var userId = randomUUID().toString();
    when(userSubscriptionProductJpaRepository.existsByUserIdAndSubscriptionEndDatetimeIsNull(
            userId))
        .thenReturn(false);

    assertThrows(
        NotFoundException.class, () -> subject.ensureActiveSubscriptionProduct(userId, null));
    verify(userSubscriptionProductJpaRepository, never()).save(any());
    verify(creditGrantService, never()).grantIncludedCredits(any(), any());
  }

  @Test
  void throws_when_plan_id_unknown() {
    var userId = randomUUID().toString();
    var unknownPlanId = "unknown_plan_id";
    when(subscriptionProductRepository.findById(unknownPlanId)).thenReturn(Optional.empty());
    when(userSubscriptionProductJpaRepository.existsByUserIdAndSubscriptionEndDatetimeIsNull(
            userId))
        .thenReturn(false);

    assertThrows(
        NotFoundException.class,
        () -> subject.ensureActiveSubscriptionProduct(userId, unknownPlanId));
    verify(userSubscriptionProductJpaRepository, never()).save(any());
    verify(creditGrantService, never()).grantIncludedCredits(any(), any());
  }

  @Test
  void ends_active_products_with_given_end_datetime() {
    var userId = randomUUID().toString();
    var endDatetime = Instant.parse("2026-09-05T00:00:00Z");
    var activeProduct =
        UserSubscriptionProduct.builder()
            .id("product_id")
            .userId(userId)
            .subscriptionStartDatetime(Instant.parse("2026-08-05T00:00:00Z"))
            .subscriptionEndDatetime(null)
            .build();
    when(userSubscriptionProductJpaRepository.findAllByUserIdAndSubscriptionEndDatetimeIsNull(
            userId))
        .thenReturn(List.of(activeProduct));
    when(userSubscriptionProductJpaRepository.saveAll(any()))
        .thenAnswer(i -> i.getArgument(0, List.class));

    var actual = subject.endActiveSubscriptionProducts(userId, endDatetime);

    @SuppressWarnings("unchecked")
    var captor = ArgumentCaptor.forClass(List.class);
    verify(userSubscriptionProductJpaRepository).saveAll(captor.capture());
    var savedProducts = (List<UserSubscriptionProduct>) captor.getValue();
    assertEquals(1, savedProducts.size());
    assertEquals(endDatetime, savedProducts.getFirst().getSubscriptionEndDatetime());
    assertEquals(1, actual.size());
    assertEquals(endDatetime, actual.getFirst().getSubscriptionEndDatetime());
  }

  @Test
  void does_nothing_when_no_active_product_to_end() {
    var userId = randomUUID().toString();
    when(userSubscriptionProductJpaRepository.findAllByUserIdAndSubscriptionEndDatetimeIsNull(
            userId))
        .thenReturn(List.of());

    var actual = subject.endActiveSubscriptionProducts(userId, Instant.now());

    assertTrue(actual.isEmpty());
    verify(userSubscriptionProductJpaRepository, never()).saveAll(any());
  }
}
