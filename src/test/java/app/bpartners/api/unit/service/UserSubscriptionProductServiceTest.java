package app.bpartners.api.unit.service;

import static app.bpartners.api.model.subscription.BillingInterval.MONTHLY;
import static app.bpartners.api.model.subscription.BillingInterval.YEARLY;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import app.bpartners.api.model.UserSubscriptionProduct;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.subscription.BillingInterval;
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

  private static UserSubscriptionProduct activeProduct(
      String userId, SubscriptionProduct plan, Instant endDatetime) {
    return activeProduct(userId, plan, endDatetime, MONTHLY);
  }

  private static UserSubscriptionProduct activeProduct(
      String userId,
      SubscriptionProduct plan,
      Instant endDatetime,
      BillingInterval billingInterval) {
    return UserSubscriptionProduct.builder()
        .id("user_subscription_product_id")
        .userId(userId)
        .subscriptionProduct(plan)
        .billingInterval(billingInterval)
        .subscriptionStartDatetime(Instant.parse("2026-08-05T00:00:00Z"))
        .subscriptionEndDatetime(endDatetime)
        .build();
  }

  @Test
  void creates_subscribed_product_with_null_end_datetime_when_no_active_one() {
    var userId = randomUUID().toString();
    var subscribedPlanId = "usage_based_plan_id";
    var subscribedPlan = SubscriptionProduct.builder().id(subscribedPlanId).build();
    when(subscriptionProductRepository.findById(subscribedPlanId))
        .thenReturn(Optional.of(subscribedPlan));
    when(userSubscriptionProductJpaRepository.findAllActiveByUserId(eq(userId), any()))
        .thenReturn(List.of());

    var actual = subject.ensureActiveSubscriptionProduct(userId, subscribedPlanId, MONTHLY);

    var captor = ArgumentCaptor.forClass(UserSubscriptionProduct.class);
    verify(userSubscriptionProductJpaRepository).save(captor.capture());
    var saved = captor.getValue();
    assertEquals(saved, actual);
    assertEquals(userId, saved.getUserId());
    assertEquals(subscribedPlanId, saved.getSubscriptionProduct().getId());
    assertEquals(MONTHLY, saved.getBillingInterval());
    assertNull(saved.getSubscriptionEndDatetime());
    assertNotNull(saved.getSubscriptionStartDatetime());
    assertNotNull(saved.getCreationDatetime());
    verify(creditGrantService).grantIncludedCredits(userId, subscribedPlan);
  }

  @Test
  void grants_included_credits_without_creating_when_already_active_on_subscribed_plan() {
    var userId = randomUUID().toString();
    var subscribedPlanId = "essential_plan_id";
    var subscribedPlan = SubscriptionProduct.builder().id(subscribedPlanId).build();
    var alreadyActive = activeProduct(userId, subscribedPlan, null);
    when(subscriptionProductRepository.findById(subscribedPlanId))
        .thenReturn(Optional.of(subscribedPlan));
    when(userSubscriptionProductJpaRepository.findAllActiveByUserId(eq(userId), any()))
        .thenReturn(List.of(alreadyActive));

    var actual = subject.ensureActiveSubscriptionProduct(userId, subscribedPlanId, MONTHLY);

    assertEquals(alreadyActive, actual);
    verify(userSubscriptionProductJpaRepository, never()).save(any());
    verify(creditGrantService).grantIncludedCredits(userId, subscribedPlan);
  }

  @Test
  void clears_scheduled_end_when_subscribed_again_to_the_same_plan() {
    var userId = randomUUID().toString();
    var subscribedPlanId = "essential_plan_id";
    var subscribedPlan = SubscriptionProduct.builder().id(subscribedPlanId).build();
    var endingSoon = activeProduct(userId, subscribedPlan, Instant.parse("2026-09-05T00:00:00Z"));
    when(subscriptionProductRepository.findById(subscribedPlanId))
        .thenReturn(Optional.of(subscribedPlan));
    when(userSubscriptionProductJpaRepository.findAllActiveByUserId(eq(userId), any()))
        .thenReturn(List.of(endingSoon));

    var actual = subject.ensureActiveSubscriptionProduct(userId, subscribedPlanId, MONTHLY);

    var captor = ArgumentCaptor.forClass(UserSubscriptionProduct.class);
    verify(userSubscriptionProductJpaRepository).save(captor.capture());
    assertNull(captor.getValue().getSubscriptionEndDatetime());
    assertNull(actual.getSubscriptionEndDatetime());
    verify(creditGrantService).grantIncludedCredits(userId, subscribedPlan);
  }

  @Test
  void ends_previous_product_and_grants_new_plan_credits_on_plan_change() {
    var userId = randomUUID().toString();
    var previousPlan = SubscriptionProduct.builder().id("previous_plan_id").build();
    var subscribedPlan = SubscriptionProduct.builder().id("upgraded_plan_id").build();
    var previouslyActive = activeProduct(userId, previousPlan, null);
    when(subscriptionProductRepository.findById("upgraded_plan_id"))
        .thenReturn(Optional.of(subscribedPlan));
    when(userSubscriptionProductJpaRepository.findAllActiveByUserId(eq(userId), any()))
        .thenReturn(List.of(previouslyActive));
    when(userSubscriptionProductJpaRepository.saveAll(any()))
        .thenAnswer(i -> i.getArgument(0, List.class));

    var actual = subject.ensureActiveSubscriptionProduct(userId, "upgraded_plan_id", MONTHLY);

    @SuppressWarnings("unchecked")
    var endedCaptor = ArgumentCaptor.forClass(List.class);
    verify(userSubscriptionProductJpaRepository).saveAll(endedCaptor.capture());
    var ended = (List<UserSubscriptionProduct>) endedCaptor.getValue();
    assertEquals(1, ended.size());
    assertNotNull(ended.getFirst().getSubscriptionEndDatetime());
    assertEquals("upgraded_plan_id", actual.getSubscriptionProduct().getId());
    assertNull(actual.getSubscriptionEndDatetime());
    verify(creditGrantService).grantIncludedCredits(userId, subscribedPlan);
    verify(creditGrantService, never()).grantIncludedCredits(userId, previousPlan);
  }

  @Test
  void creates_subscribed_product_with_yearly_billing_interval() {
    var userId = randomUUID().toString();
    var subscribedPlanId = "essential_plan_id";
    var subscribedPlan = SubscriptionProduct.builder().id(subscribedPlanId).build();
    when(subscriptionProductRepository.findById(subscribedPlanId))
        .thenReturn(Optional.of(subscribedPlan));
    when(userSubscriptionProductJpaRepository.findAllActiveByUserId(eq(userId), any()))
        .thenReturn(List.of());

    var actual = subject.ensureActiveSubscriptionProduct(userId, subscribedPlanId, YEARLY);

    assertEquals(YEARLY, actual.getBillingInterval());
  }

  @Test
  void creates_subscribed_product_billed_monthly_when_billing_interval_unknown() {
    var userId = randomUUID().toString();
    var subscribedPlanId = "essential_plan_id";
    var subscribedPlan = SubscriptionProduct.builder().id(subscribedPlanId).build();
    when(subscriptionProductRepository.findById(subscribedPlanId))
        .thenReturn(Optional.of(subscribedPlan));
    when(userSubscriptionProductJpaRepository.findAllActiveByUserId(eq(userId), any()))
        .thenReturn(List.of());

    var actual = subject.ensureActiveSubscriptionProduct(userId, subscribedPlanId, null);

    assertEquals(MONTHLY, actual.getBillingInterval());
  }

  @Test
  void updates_billing_interval_when_same_plan_is_billed_on_another_interval() {
    var userId = randomUUID().toString();
    var subscribedPlanId = "essential_plan_id";
    var subscribedPlan = SubscriptionProduct.builder().id(subscribedPlanId).build();
    var billedMonthly = activeProduct(userId, subscribedPlan, null, MONTHLY);
    when(subscriptionProductRepository.findById(subscribedPlanId))
        .thenReturn(Optional.of(subscribedPlan));
    when(userSubscriptionProductJpaRepository.findAllActiveByUserId(eq(userId), any()))
        .thenReturn(List.of(billedMonthly));

    var actual = subject.ensureActiveSubscriptionProduct(userId, subscribedPlanId, YEARLY);

    var captor = ArgumentCaptor.forClass(UserSubscriptionProduct.class);
    verify(userSubscriptionProductJpaRepository).save(captor.capture());
    assertEquals(YEARLY, captor.getValue().getBillingInterval());
    assertEquals(YEARLY, actual.getBillingInterval());
    verify(creditGrantService).grantIncludedCredits(userId, subscribedPlan);
  }

  @Test
  void keeps_billing_interval_when_same_plan_is_billed_on_the_same_interval() {
    var userId = randomUUID().toString();
    var subscribedPlanId = "essential_plan_id";
    var subscribedPlan = SubscriptionProduct.builder().id(subscribedPlanId).build();
    var billedYearly = activeProduct(userId, subscribedPlan, null, YEARLY);
    when(subscriptionProductRepository.findById(subscribedPlanId))
        .thenReturn(Optional.of(subscribedPlan));
    when(userSubscriptionProductJpaRepository.findAllActiveByUserId(eq(userId), any()))
        .thenReturn(List.of(billedYearly));

    var actual = subject.ensureActiveSubscriptionProduct(userId, subscribedPlanId, YEARLY);

    assertEquals(billedYearly, actual);
    verify(userSubscriptionProductJpaRepository, never()).save(any());
  }

  @Test
  void grants_from_already_active_plan_when_plan_id_null() {
    var userId = randomUUID().toString();
    var activePlan = SubscriptionProduct.builder().id("active_plan_id").build();
    var alreadyActive = activeProduct(userId, activePlan, null);
    when(userSubscriptionProductJpaRepository.findAllActiveByUserId(eq(userId), any()))
        .thenReturn(List.of(alreadyActive));

    var actual = subject.ensureActiveSubscriptionProduct(userId, null, null);

    assertEquals(alreadyActive, actual);
    verify(userSubscriptionProductJpaRepository, never()).save(any());
    verify(creditGrantService).grantIncludedCredits(userId, activePlan);
  }

  @Test
  void throws_when_plan_id_null_and_no_active_product() {
    var userId = randomUUID().toString();
    when(userSubscriptionProductJpaRepository.findAllActiveByUserId(eq(userId), any()))
        .thenReturn(List.of());

    assertThrows(
        NotFoundException.class, () -> subject.ensureActiveSubscriptionProduct(userId, null, null));
    verify(userSubscriptionProductJpaRepository, never()).save(any());
    verify(creditGrantService, never()).grantIncludedCredits(any(), any());
  }

  @Test
  void throws_when_plan_id_unknown() {
    var userId = randomUUID().toString();
    var unknownPlanId = "unknown_plan_id";
    when(subscriptionProductRepository.findById(unknownPlanId)).thenReturn(Optional.empty());
    when(userSubscriptionProductJpaRepository.findAllActiveByUserId(eq(userId), any()))
        .thenReturn(List.of());

    assertThrows(
        NotFoundException.class,
        () -> subject.ensureActiveSubscriptionProduct(userId, unknownPlanId, MONTHLY));
    verify(userSubscriptionProductJpaRepository, never()).save(any());
    verify(creditGrantService, never()).grantIncludedCredits(any(), any());
  }

  @Test
  void ends_active_products_with_given_end_datetime() {
    var userId = randomUUID().toString();
    var endDatetime = Instant.parse("2026-09-05T00:00:00Z");
    var product = activeProduct(userId, SubscriptionProduct.builder().id("plan_id").build(), null);
    when(userSubscriptionProductJpaRepository.findAllActiveByUserId(eq(userId), any()))
        .thenReturn(List.of(product));
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
    when(userSubscriptionProductJpaRepository.findAllActiveByUserId(eq(userId), any()))
        .thenReturn(List.of());

    var actual = subject.endActiveSubscriptionProducts(userId, Instant.now());

    assertTrue(actual.isEmpty());
    verify(userSubscriptionProductJpaRepository, never()).saveAll(any());
  }
}
