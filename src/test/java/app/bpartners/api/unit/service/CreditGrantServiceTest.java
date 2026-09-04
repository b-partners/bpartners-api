package app.bpartners.api.unit.service;

import static app.bpartners.api.model.credit.CreditTransactionMovementType.CREDIT;
import static app.bpartners.api.model.credit.CreditTransactionType.SUBSCRIPTION_GRANT;
import static java.time.Instant.now;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.model.credit.CreditTransaction;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.repository.jpa.CreditTransactionRepository;
import app.bpartners.api.service.credit.CreditGrantService;
import app.bpartners.api.service.credit.CreditLedgerService;
import app.bpartners.api.service.utils.TemporalUtils;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CreditGrantServiceTest {
  CreditLedgerService creditLedgerService = mock(CreditLedgerService.class);
  CreditTransactionRepository creditTransactionRepository = mock(CreditTransactionRepository.class);
  TemporalUtils temporalUtils = new TemporalUtils();
  CreditGrantService subject =
      new CreditGrantService(creditLedgerService, creditTransactionRepository, temporalUtils);

  private static SubscriptionProduct plan(Long includedCredits) {
    return plan("plan_id", includedCredits);
  }

  private static SubscriptionProduct plan(String id, Long includedCredits) {
    return SubscriptionProduct.builder()
        .id(id)
        .name("Essentiel")
        .includedCreditsPerBillingPeriod(includedCredits)
        .build();
  }

  @BeforeEach
  void setUp() {
    when(creditLedgerService.append(any()))
        .thenAnswer(i -> i.getArgument(0, CreditTransaction.class).toBuilder().id("tx_id").build());
    when(creditTransactionRepository
            .existsByUserIdAndTypeAndSubscriptionProductIdAndGrantPeriodStart(
                any(), any(), any(), any()))
        .thenReturn(false);
  }

  @Test
  void grants_included_credits_expiring_at_the_end_of_the_billing_period() {
    var actual = subject.grantIncludedCredits("user_id", plan(10L));

    var captor = ArgumentCaptor.forClass(CreditTransaction.class);
    verify(creditLedgerService).append(captor.capture());
    var appended = captor.getValue();
    assertTrue(actual.isPresent());
    assertEquals("tx_id", actual.orElseThrow().getId());
    assertEquals("user_id", appended.getUserId());
    assertEquals(SUBSCRIPTION_GRANT, appended.getType());
    assertEquals(CREDIT, appended.getMovementType());
    assertEquals(10L, appended.getCredits());
    assertEquals("Crédits inclus dans l'abonnement Essentiel", appended.getLabel());
    assertEquals("plan_id", appended.getSubscriptionProductId());
    assertEquals(temporalUtils.startOfActualMonth(), appended.getGrantPeriodStart());
    assertEquals(temporalUtils.startOfNextMonthInstant(), appended.getExpirationDatetime());
  }

  @Test
  void grants_nothing_when_plan_includes_zero_credit() {
    var actual = subject.grantIncludedCredits("user_id", plan(0L));

    assertTrue(actual.isEmpty());
    verify(creditLedgerService, never()).append(any());
  }

  @Test
  void grants_nothing_when_plan_includes_no_credit() {
    var actual = subject.grantIncludedCredits("user_id", plan(null));

    assertTrue(actual.isEmpty());
    verify(creditLedgerService, never()).append(any());
  }

  @Test
  void grants_nothing_when_the_same_plan_was_already_granted_for_the_billing_period() {
    when(creditTransactionRepository
            .existsByUserIdAndTypeAndSubscriptionProductIdAndGrantPeriodStart(
                "user_id", SUBSCRIPTION_GRANT, "plan_id", temporalUtils.startOfActualMonth()))
        .thenReturn(true);

    var actual = subject.grantIncludedCredits("user_id", plan(10L));

    assertTrue(actual.isEmpty());
    verify(creditLedgerService, never()).append(any());
  }

  @Test
  void grants_again_when_another_plan_was_granted_for_the_same_billing_period() {
    when(creditTransactionRepository
            .existsByUserIdAndTypeAndSubscriptionProductIdAndGrantPeriodStart(
                "user_id",
                SUBSCRIPTION_GRANT,
                "previous_plan_id",
                temporalUtils.startOfActualMonth()))
        .thenReturn(true);

    var actual = subject.grantIncludedCredits("user_id", plan("upgraded_plan_id", 30L));

    var captor = ArgumentCaptor.forClass(CreditTransaction.class);
    verify(creditLedgerService).append(captor.capture());
    assertTrue(actual.isPresent());
    assertEquals(30L, captor.getValue().getCredits());
    assertEquals("upgraded_plan_id", captor.getValue().getSubscriptionProductId());
  }

  @Test
  void labels_the_grant_without_plan_name_when_plan_is_unnamed() {
    subject.grantIncludedCredits("user_id", plan(10L).toBuilder().name(null).build());

    var captor = ArgumentCaptor.forClass(CreditTransaction.class);
    verify(creditLedgerService).append(captor.capture());
    assertEquals("Crédits inclus dans l'abonnement", captor.getValue().getLabel());
  }

  @Test
  void grants_transitional_credits_without_plan_expiring_at_end_of_month() {
    var actual = subject.grantTransitionalCredits("user_id", 25L);

    var captor = ArgumentCaptor.forClass(CreditTransaction.class);
    verify(creditLedgerService).append(captor.capture());
    var appended = captor.getValue();
    assertTrue(actual.isPresent());
    assertEquals(SUBSCRIPTION_GRANT, appended.getType());
    assertEquals(CREDIT, appended.getMovementType());
    assertEquals(25L, appended.getCredits());
    assertNull(appended.getSubscriptionProductId());
    assertEquals("Crédits offerts pendant la transition vers le prépayé", appended.getLabel());
    assertEquals(temporalUtils.startOfActualMonth(), appended.getGrantPeriodStart());
    assertEquals(temporalUtils.startOfNextMonthInstant(), appended.getExpirationDatetime());
  }

  @Test
  void grants_no_transitional_credits_when_amount_is_not_positive() {
    var actual = subject.grantTransitionalCredits("user_id", 0L);

    assertTrue(actual.isEmpty());
    verify(creditLedgerService, never()).append(any());
  }

  @Test
  void skips_transitional_grant_when_a_live_transitional_grant_already_exists() {
    when(creditTransactionRepository.findAllByUserIdAndTypeAndSubscriptionProductIdIsNull(
            "user_id", SUBSCRIPTION_GRANT))
        .thenReturn(List.of(liveTransitionalGrant()));

    var actual = subject.grantTransitionalCredits("user_id", 25L);

    assertTrue(actual.isEmpty());
    verify(creditLedgerService, never()).append(any());
  }

  @Test
  void revokes_live_transitional_grants_by_expiring_them_now() {
    when(creditTransactionRepository.findAllByUserIdAndTypeAndSubscriptionProductIdIsNull(
            "user_id", SUBSCRIPTION_GRANT))
        .thenReturn(List.of(liveTransitionalGrant()));

    subject.revokeTransitionalGrants("user_id");

    verify(creditTransactionRepository).acquireWalletLock("user_id");
    var captor = ArgumentCaptor.forClass(List.class);
    verify(creditTransactionRepository).saveAll(captor.capture());
    @SuppressWarnings("unchecked")
    var revoked = (List<CreditTransaction>) captor.getValue();
    assertEquals(1, revoked.size());
    assertFalse(revoked.getFirst().isExpiredAt(now().minus(1, ChronoUnit.SECONDS)));
    assertTrue(revoked.getFirst().isExpiredAt(now().plus(1, ChronoUnit.SECONDS)));
  }

  @Test
  void revokes_nothing_when_no_live_transitional_grant_exists() {
    subject.revokeTransitionalGrants("user_id");

    verify(creditTransactionRepository, never()).saveAll(any());
  }

  private static CreditTransaction liveTransitionalGrant() {
    return CreditTransaction.builder()
        .id("transitional_tx_id")
        .userId("user_id")
        .type(SUBSCRIPTION_GRANT)
        .movementType(CREDIT)
        .credits(25L)
        .subscriptionProductId(null)
        .expirationDatetime(now().plus(20, ChronoUnit.DAYS))
        .build();
  }
}
