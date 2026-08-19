package app.bpartners.api.unit;

import static app.bpartners.api.model.credit.CreditCode.ANALYSES_10;
import static app.bpartners.api.model.credit.CreditOrigin.SUBSCRIPTION_GRANT;
import static app.bpartners.api.model.credit.CreditTransactionMovementType.CREDIT;
import static app.bpartners.api.model.credit.CreditTransactionMovementType.DEBIT;
import static app.bpartners.api.model.credit.CreditTransactionType.CONSUMPTION;
import static app.bpartners.api.model.credit.CreditTransactionType.PURCHASE;
import static app.bpartners.api.model.subscription.SubscriptionBillingType.USAGE_BASED;
import static java.time.temporal.ChronoUnit.DAYS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.model.User;
import app.bpartners.api.model.UserSubscriptionProduct;
import app.bpartners.api.model.credit.CreditPack;
import app.bpartners.api.model.credit.CreditPurchaseType;
import app.bpartners.api.model.credit.CreditTransaction;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.subscription.SubscriptionProduct;
import app.bpartners.api.repository.jpa.CreditPackRepository;
import app.bpartners.api.repository.jpa.CreditTransactionRepository;
import app.bpartners.api.repository.jpa.SubscriptionProductRepository;
import app.bpartners.api.repository.jpa.UserSubscriptionProductJpaRepository;
import app.bpartners.api.service.credit.CreditLedgerService;
import app.bpartners.api.service.credit.CreditService;
import app.bpartners.api.service.utils.TemporalUtils;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CreditServiceTest {
  CreditPackRepository creditPackRepository = mock(CreditPackRepository.class);
  UserSubscriptionProductJpaRepository userSubscriptionProductJpaRepository =
      mock(UserSubscriptionProductJpaRepository.class);
  SubscriptionProductRepository subscriptionProductRepository =
      mock(SubscriptionProductRepository.class);
  CreditTransactionRepository creditTransactionRepository = mock(CreditTransactionRepository.class);
  CreditLedgerService creditLedgerService = mock(CreditLedgerService.class);
  TemporalUtils temporalUtils = new TemporalUtils();

  CreditService subject =
      new CreditService(
          creditPackRepository,
          userSubscriptionProductJpaRepository,
          subscriptionProductRepository,
          creditTransactionRepository,
          creditLedgerService,
          temporalUtils);

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

  @Test
  void get_credit_balance_returns_zeros_on_empty_ledger() {
    when(creditTransactionRepository.findAllByUserId("user_id")).thenReturn(List.of());
    when(userSubscriptionProductJpaRepository.findAllByUserIdAndSubscriptionEndDatetimeIsNull(
            "user_id"))
        .thenReturn(List.of());

    var actual = subject.getCreditBalance("user_id");

    assertEquals(0L, actual.getSpendableCredits());
    assertEquals(0L, actual.getGrantedCredits());
    assertEquals(0L, actual.getPurchasedCredits());
    assertEquals(1L, actual.getCreditCostPerAnalysis());
    assertEquals(0L, actual.getEstimatedRemainingAnalyses());
    assertTrue(actual.getExpirations().isEmpty());
    assertNull(actual.getUpdatedAt());
    assertNull(actual.getNextGrantDatetime());
  }

  @Test
  void get_credit_balance_splits_origins_debits_soonest_expiry_and_lists_expirations() {
    var now = Instant.now();
    var grantExpiry = now.plus(10, DAYS);
    when(creditTransactionRepository.findAllByUserId("user_id"))
        .thenReturn(
            List.of(
                CreditTransaction.builder()
                    .type(app.bpartners.api.model.credit.CreditTransactionType.SUBSCRIPTION_GRANT)
                    .movementType(CREDIT)
                    .credits(20L)
                    .expirationDatetime(grantExpiry)
                    .creationDatetime(now.minus(2, DAYS))
                    .build(),
                CreditTransaction.builder()
                    .type(PURCHASE)
                    .movementType(CREDIT)
                    .credits(30L)
                    .creationDatetime(now.minus(1, DAYS))
                    .build(),
                CreditTransaction.builder()
                    .type(CONSUMPTION)
                    .movementType(DEBIT)
                    .credits(5L)
                    .creationDatetime(now)
                    .build()));
    when(userSubscriptionProductJpaRepository.findAllByUserIdAndSubscriptionEndDatetimeIsNull(
            "user_id"))
        .thenReturn(
            List.of(
                UserSubscriptionProduct.builder()
                    .subscriptionProduct(
                        SubscriptionProduct.builder().creditCostPerAnalysis(2L).build())
                    .build()));

    var actual = subject.getCreditBalance("user_id");

    assertEquals(15L, actual.getGrantedCredits());
    assertEquals(30L, actual.getPurchasedCredits());
    assertEquals(45L, actual.getSpendableCredits());
    assertEquals(2L, actual.getCreditCostPerAnalysis());
    assertEquals(22L, actual.getEstimatedRemainingAnalyses());
    assertEquals(now, actual.getUpdatedAt());
    assertEquals(1, actual.getExpirations().size());
    assertEquals(15L, actual.getExpirations().getFirst().getCredits());
    assertEquals(grantExpiry, actual.getExpirations().getFirst().getExpirationDatetime());
    assertEquals(SUBSCRIPTION_GRANT, actual.getExpirations().getFirst().getOrigin());
  }

  @Test
  void consume_roof_analysis_debits_plan_cost_and_appends_consumption() {
    when(userSubscriptionProductJpaRepository.findAllByUserIdAndSubscriptionEndDatetimeIsNull(
            "user_id"))
        .thenReturn(
            List.of(
                UserSubscriptionProduct.builder()
                    .subscriptionProduct(
                        SubscriptionProduct.builder().creditCostPerAnalysis(3L).build())
                    .build()));
    var captor = ArgumentCaptor.forClass(CreditTransaction.class);
    when(creditLedgerService.append(any())).thenAnswer(invocation -> invocation.getArgument(0));

    subject.consumeRoofAnalysis("user_id", "Analyse de toiture");

    verify(creditLedgerService).append(captor.capture());
    var appended = captor.getValue();
    assertEquals("user_id", appended.getUserId());
    assertEquals(CONSUMPTION, appended.getType());
    assertEquals(DEBIT, appended.getMovementType());
    assertEquals(3L, appended.getCredits());
    assertEquals("Analyse de toiture", appended.getLabel());
  }

  @Test
  void consume_roof_analysis_falls_back_to_default_cost_when_no_active_plan() {
    when(userSubscriptionProductJpaRepository.findAllByUserIdAndSubscriptionEndDatetimeIsNull(
            "user_id"))
        .thenReturn(List.of());
    var appended = CreditTransaction.builder().id("appended_id").build();
    when(creditLedgerService.append(any())).thenReturn(appended);
    var captor = ArgumentCaptor.forClass(CreditTransaction.class);

    var actual = subject.consumeRoofAnalysis("user_id", "Analyse toiture : dummyAddress");

    verify(creditLedgerService).append(captor.capture());
    assertEquals(1L, captor.getValue().getCredits());
    assertEquals(appended, actual);
  }

  @Test
  void consume_roof_analysis_falls_back_to_default_cost_when_active_plan_has_no_credit_cost() {
    when(userSubscriptionProductJpaRepository.findAllByUserIdAndSubscriptionEndDatetimeIsNull(
            "user_id"))
        .thenReturn(
            List.of(
                UserSubscriptionProduct.builder()
                    .subscriptionProduct(SubscriptionProduct.builder().build())
                    .build()));
    when(creditLedgerService.append(any())).thenAnswer(invocation -> invocation.getArgument(0));
    var captor = ArgumentCaptor.forClass(CreditTransaction.class);

    subject.consumeRoofAnalysis("user_id", "Analyse toiture : dummyAddress");

    verify(creditLedgerService).append(captor.capture());
    assertEquals(1L, captor.getValue().getCredits());
  }
}
