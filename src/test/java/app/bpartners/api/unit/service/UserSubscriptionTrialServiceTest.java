package app.bpartners.api.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.model.UserSubscriptionProduct;
import app.bpartners.api.model.UserSubscriptionTrial;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.ConflictException;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.repository.jpa.SubscriptionProductRepository;
import app.bpartners.api.repository.jpa.UserSubscriptionTrialJpaRepository;
import app.bpartners.api.service.credit.CreditGrantService;
import app.bpartners.api.service.subscription.UserSubscriptionProductService;
import app.bpartners.api.service.subscription.UserSubscriptionTrialService;
import java.time.ZoneId;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class UserSubscriptionTrialServiceTest {
  UserSubscriptionTrialJpaRepository userSubscriptionTrialJpaRepository = mock();
  SubscriptionProductRepository subscriptionProductRepository = mock();
  UserSubscriptionProductService userSubscriptionProductService = mock();
  CreditGrantService creditGrantService = mock();
  UserSubscriptionTrialService subject =
      new UserSubscriptionTrialService(
          userSubscriptionTrialJpaRepository,
          subscriptionProductRepository,
          userSubscriptionProductService,
          creditGrantService);

  private static SubscriptionProduct trialPlan(Integer trialPeriodDays) {
    return SubscriptionProduct.builder()
        .id("plan_id")
        .name("Essentiel")
        .trialPeriodDays(trialPeriodDays)
        .trialAnalysisGranted(2)
        .creditCostPerAnalysis(3L)
        .build();
  }

  @BeforeEach
  void setUp() {
    when(subscriptionProductRepository.findById("plan_id")).thenReturn(Optional.of(trialPlan(7)));
    when(userSubscriptionTrialJpaRepository.existsByUserIdAndSubscriptionProductId(
            "user_id", "plan_id"))
        .thenReturn(false);
    when(userSubscriptionProductService.findActiveUserSubscriptionProduct("user_id"))
        .thenReturn(Optional.empty());
    when(userSubscriptionTrialJpaRepository.save(any())).thenAnswer(i -> i.getArgument(0));
  }

  @Test
  void starts_trial_opens_association_grants_credits_and_records_it() {
    var actual = subject.startTrial("user_id", "plan_id");

    var trialCaptor = ArgumentCaptor.forClass(UserSubscriptionTrial.class);
    verify(userSubscriptionTrialJpaRepository).save(trialCaptor.capture());
    var savedTrial = trialCaptor.getValue();
    var expectedExpiry =
        savedTrial.getStartedAt().atZone(ZoneId.of("Europe/Paris")).plusDays(7).toInstant();
    assertEquals("user_id", savedTrial.getUserId());
    assertEquals("plan_id", savedTrial.getSubscriptionProductId());
    assertEquals(expectedExpiry, savedTrial.getExpiresAt());
    assertEquals(2L, actual.grantedAnalyses());
    assertEquals(6L, actual.grantedCredits());
    verify(userSubscriptionProductService)
        .createTrialAssociation(
            eq("user_id"), any(SubscriptionProduct.class), any(), eq(expectedExpiry));
    verify(creditGrantService)
        .grantTrialCredits(eq("user_id"), any(SubscriptionProduct.class), eq(expectedExpiry));
  }

  @Test
  void rejects_when_plan_offers_no_trial() {
    when(subscriptionProductRepository.findById("plan_id")).thenReturn(Optional.of(trialPlan(0)));

    assertThrows(BadRequestException.class, () -> subject.startTrial("user_id", "plan_id"));
    verify(userSubscriptionProductService, never())
        .createTrialAssociation(any(), any(), any(), any());
    verify(creditGrantService, never()).grantTrialCredits(any(), any(), any());
  }

  @Test
  void rejects_when_trial_already_used_on_the_product() {
    when(userSubscriptionTrialJpaRepository.existsByUserIdAndSubscriptionProductId(
            "user_id", "plan_id"))
        .thenReturn(true);

    assertThrows(ConflictException.class, () -> subject.startTrial("user_id", "plan_id"));
    verify(userSubscriptionProductService, never())
        .createTrialAssociation(any(), any(), any(), any());
    verify(creditGrantService, never()).grantTrialCredits(any(), any(), any());
  }

  @Test
  void rejects_when_user_already_has_an_active_subscription() {
    when(userSubscriptionProductService.findActiveUserSubscriptionProduct("user_id"))
        .thenReturn(Optional.of(UserSubscriptionProduct.builder().id("usp_id").build()));

    assertThrows(ConflictException.class, () -> subject.startTrial("user_id", "plan_id"));
    verify(userSubscriptionProductService, never())
        .createTrialAssociation(any(), any(), any(), any());
    verify(creditGrantService, never()).grantTrialCredits(any(), any(), any());
  }
}
