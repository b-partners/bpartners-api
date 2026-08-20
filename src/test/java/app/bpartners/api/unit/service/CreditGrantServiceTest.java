package app.bpartners.api.unit.service;

import static app.bpartners.api.model.credit.CreditTransactionMovementType.CREDIT;
import static app.bpartners.api.model.credit.CreditTransactionType.SUBSCRIPTION_GRANT;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
    return SubscriptionProduct.builder()
        .id("plan_id")
        .name("Essentiel")
        .includedCreditsPerBillingPeriod(includedCredits)
        .build();
  }

  @BeforeEach
  void setUp() {
    when(creditLedgerService.append(any()))
        .thenAnswer(i -> i.getArgument(0, CreditTransaction.class).toBuilder().id("tx_id").build());
    when(creditTransactionRepository.existsByUserIdAndTypeAndCreationDatetimeGreaterThanEqual(
            any(), any(), any()))
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
  void grants_nothing_when_the_billing_period_was_already_granted() {
    when(creditTransactionRepository.existsByUserIdAndTypeAndCreationDatetimeGreaterThanEqual(
            "user_id", SUBSCRIPTION_GRANT, temporalUtils.startOfMonth()))
        .thenReturn(true);

    var actual = subject.grantIncludedCredits("user_id", plan(10L));

    assertTrue(actual.isEmpty());
    verify(creditLedgerService, never()).append(any());
  }

  @Test
  void labels_the_grant_without_plan_name_when_plan_is_unnamed() {
    subject.grantIncludedCredits("user_id", plan(10L).toBuilder().name(null).build());

    var captor = ArgumentCaptor.forClass(CreditTransaction.class);
    verify(creditLedgerService).append(captor.capture());
    assertEquals("Crédits inclus dans l'abonnement", captor.getValue().getLabel());
  }
}
