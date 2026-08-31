package app.bpartners.api.integration;

import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ID;
import static app.bpartners.api.model.credit.CreditTransactionMovementType.CREDIT;
import static app.bpartners.api.model.credit.CreditTransactionType.SUBSCRIPTION_GRANT;
import static app.bpartners.api.model.subscription.BillingInterval.MONTHLY;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import app.bpartners.api.endpoint.event.model.MonthlySubscriptionCreditGrantRequested;
import app.bpartners.api.endpoint.event.model.MonthlySubscriptionCreditGrantTriggered;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.model.credit.CreditTransaction;
import app.bpartners.api.repository.jpa.CreditTransactionRepository;
import app.bpartners.api.repository.jpa.UserSubscriptionProductJpaRepository;
import app.bpartners.api.service.credit.CreditService;
import app.bpartners.api.service.event.MonthlySubscriptionCreditGrantRequestedService;
import app.bpartners.api.service.event.MonthlySubscriptionCreditGrantTriggeredService;
import app.bpartners.api.service.subscription.UserSubscriptionProductService;
import app.bpartners.api.service.utils.TemporalUtils;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;

class SubscriptionCreditGrantIT extends MockedThirdParties {
  private static final String ESSENTIAL_PLAN_ID = "89f1acdd-c3b9-4717-a21d-355b2021ad58";
  private static final String USAGE_BASED_PLAN_ID = "4219611e-7584-4636-a3c5-ba212600715b";
  private static final String PRO_PLAN_ID = "c5f57306-a7b1-43f4-90fc-204ccd4c0ce2";
  private static final long ESSENTIAL_INCLUDED_CREDITS = 10L;
  private static final long PRO_INCLUDED_CREDITS = 25L;

  @Autowired private UserSubscriptionProductService userSubscriptionProductService;
  @Autowired private UserSubscriptionProductJpaRepository userSubscriptionProductJpaRepository;
  @Autowired private CreditTransactionRepository creditTransactionRepository;
  @Autowired private CreditService creditService;
  @Autowired private TemporalUtils temporalUtils;
  @Autowired private MonthlySubscriptionCreditGrantTriggeredService grantTriggeredService;
  @Autowired private MonthlySubscriptionCreditGrantRequestedService grantRequestedService;

  @BeforeEach
  void setUp() {
    clearSubscriptionsAndLedger();
  }

  @AfterEach
  void tearDown() {
    clearSubscriptionsAndLedger();
  }

  private void clearSubscriptionsAndLedger() {
    creditTransactionRepository.deleteAll();
    userSubscriptionProductJpaRepository.deleteAll();
  }

  private void replaceLedgerByAPreviousBillingPeriodGrant() {
    creditTransactionRepository.deleteAll();
    creditTransactionRepository.save(
        CreditTransaction.builder()
            .id(randomUUID().toString())
            .userId(JOE_DOE_ID)
            .type(SUBSCRIPTION_GRANT)
            .movementType(CREDIT)
            .credits(ESSENTIAL_INCLUDED_CREDITS)
            .label("Crédits inclus dans l'abonnement Essentiel")
            .subscriptionProductId(ESSENTIAL_PLAN_ID)
            .grantPeriodStart(temporalUtils.startOfLastMonth())
            .expirationDatetime(temporalUtils.startOfMonth())
            .creationDatetime(temporalUtils.startOfLastMonthInstant())
            .build());
  }

  @Test
  void subscribing_grants_the_credits_included_in_the_plan() {
    userSubscriptionProductService.ensureActiveSubscriptionProduct(
        JOE_DOE_ID, ESSENTIAL_PLAN_ID, MONTHLY);

    var ledger = creditTransactionRepository.findAllByUserId(JOE_DOE_ID);
    assertEquals(1, ledger.size());
    var grant = ledger.getFirst();
    assertEquals(SUBSCRIPTION_GRANT, grant.getType());
    assertEquals(CREDIT, grant.getMovementType());
    assertEquals(ESSENTIAL_INCLUDED_CREDITS, grant.getCredits());
    assertEquals(temporalUtils.startOfNextMonthInstant(), grant.getExpirationDatetime());

    var balance = creditService.getCreditBalance(JOE_DOE_ID);
    assertEquals(ESSENTIAL_INCLUDED_CREDITS, balance.getGrantedCredits());
    assertEquals(ESSENTIAL_INCLUDED_CREDITS, balance.getSpendableCredits());
    assertEquals(0L, balance.getPurchasedCredits());
    assertEquals(temporalUtils.startOfNextMonthInstant(), balance.getNextGrantDatetime());
  }

  @Test
  void subscribing_twice_grants_the_included_credits_only_once() {
    userSubscriptionProductService.ensureActiveSubscriptionProduct(
        JOE_DOE_ID, ESSENTIAL_PLAN_ID, MONTHLY);
    userSubscriptionProductService.ensureActiveSubscriptionProduct(
        JOE_DOE_ID, ESSENTIAL_PLAN_ID, MONTHLY);

    assertEquals(1, creditTransactionRepository.findAllByUserId(JOE_DOE_ID).size());
    assertEquals(
        ESSENTIAL_INCLUDED_CREDITS, creditService.getCreditBalance(JOE_DOE_ID).getGrantedCredits());
  }

  @Test
  void subscribing_to_a_plan_without_included_credits_grants_nothing() {
    userSubscriptionProductService.ensureActiveSubscriptionProduct(
        JOE_DOE_ID, USAGE_BASED_PLAN_ID, MONTHLY);

    assertTrue(creditTransactionRepository.findAllByUserId(JOE_DOE_ID).isEmpty());
    assertEquals(0L, creditService.getCreditBalance(JOE_DOE_ID).getGrantedCredits());
  }

  @Test
  void monthly_trigger_requests_a_grant_for_every_subscribed_user() {
    userSubscriptionProductService.ensureActiveSubscriptionProduct(
        JOE_DOE_ID, ESSENTIAL_PLAN_ID, MONTHLY);

    grantTriggeredService.accept(new MonthlySubscriptionCreditGrantTriggered());

    var captor = ArgumentCaptor.forClass(List.class);
    verify(eventProducer).accept(captor.capture());
    @SuppressWarnings("unchecked")
    var requests = (List<MonthlySubscriptionCreditGrantRequested>) captor.getValue();
    assertEquals(1, requests.size());
    assertEquals(JOE_DOE_ID, requests.getFirst().getUserId());
  }

  @Test
  void renewal_grants_the_included_credits_again_for_the_new_billing_period() {
    userSubscriptionProductService.ensureActiveSubscriptionProduct(
        JOE_DOE_ID, ESSENTIAL_PLAN_ID, MONTHLY);
    replaceLedgerByAPreviousBillingPeriodGrant();

    grantRequestedService.accept(
        MonthlySubscriptionCreditGrantRequested.builder().userId(JOE_DOE_ID).build());

    var renewalGrants =
        creditTransactionRepository.findAllByUserId(JOE_DOE_ID).stream()
            .filter(
                transaction ->
                    temporalUtils
                        .startOfNextMonthInstant()
                        .equals(transaction.getExpirationDatetime()))
            .toList();
    assertEquals(2, creditTransactionRepository.findAllByUserId(JOE_DOE_ID).size());
    assertEquals(1, renewalGrants.size());
    assertEquals(SUBSCRIPTION_GRANT, renewalGrants.getFirst().getType());
    assertEquals(ESSENTIAL_INCLUDED_CREDITS, renewalGrants.getFirst().getCredits());
    assertEquals(
        ESSENTIAL_INCLUDED_CREDITS, creditService.getCreditBalance(JOE_DOE_ID).getGrantedCredits());
  }

  @Test
  void renewal_grants_nothing_twice_in_the_same_billing_period() {
    userSubscriptionProductService.ensureActiveSubscriptionProduct(
        JOE_DOE_ID, ESSENTIAL_PLAN_ID, MONTHLY);

    grantRequestedService.accept(
        MonthlySubscriptionCreditGrantRequested.builder().userId(JOE_DOE_ID).build());

    assertEquals(1, creditTransactionRepository.findAllByUserId(JOE_DOE_ID).size());
    assertEquals(
        ESSENTIAL_INCLUDED_CREDITS, creditService.getCreditBalance(JOE_DOE_ID).getGrantedCredits());
  }

  @Test
  void changing_the_plan_grants_the_credits_included_in_the_newly_subscribed_plan() {
    userSubscriptionProductService.ensureActiveSubscriptionProduct(
        JOE_DOE_ID, ESSENTIAL_PLAN_ID, MONTHLY);

    userSubscriptionProductService.ensureActiveSubscriptionProduct(
        JOE_DOE_ID, PRO_PLAN_ID, MONTHLY);

    assertEquals(2, creditTransactionRepository.findAllByUserId(JOE_DOE_ID).size());
    assertEquals(
        ESSENTIAL_INCLUDED_CREDITS + PRO_INCLUDED_CREDITS,
        creditService.getCreditBalance(JOE_DOE_ID).getGrantedCredits());
    var activeProducts =
        userSubscriptionProductJpaRepository.findAllActiveByUserId(JOE_DOE_ID, Instant.now());
    assertEquals(1, activeProducts.size());
    assertEquals(PRO_PLAN_ID, activeProducts.getFirst().getSubscriptionProduct().getId());
  }

  @Test
  void renewal_still_grants_while_the_cancelled_subscription_is_paid_until_the_period_end() {
    userSubscriptionProductService.ensureActiveSubscriptionProduct(
        JOE_DOE_ID, ESSENTIAL_PLAN_ID, MONTHLY);
    replaceLedgerByAPreviousBillingPeriodGrant();
    userSubscriptionProductService.endActiveSubscriptionProducts(
        JOE_DOE_ID, temporalUtils.endOfMonth());

    grantTriggeredService.accept(new MonthlySubscriptionCreditGrantTriggered());
    grantRequestedService.accept(
        MonthlySubscriptionCreditGrantRequested.builder().userId(JOE_DOE_ID).build());

    var captor = ArgumentCaptor.forClass(List.class);
    verify(eventProducer).accept(captor.capture());
    @SuppressWarnings("unchecked")
    var requests = (List<MonthlySubscriptionCreditGrantRequested>) captor.getValue();
    assertEquals(1, requests.size());
    assertEquals(JOE_DOE_ID, requests.getFirst().getUserId());
    assertEquals(2, creditTransactionRepository.findAllByUserId(JOE_DOE_ID).size());
    assertEquals(
        ESSENTIAL_INCLUDED_CREDITS, creditService.getCreditBalance(JOE_DOE_ID).getGrantedCredits());
  }

  @Test
  void renewal_grants_nothing_once_the_subscription_ended() {
    userSubscriptionProductService.ensureActiveSubscriptionProduct(
        JOE_DOE_ID, ESSENTIAL_PLAN_ID, MONTHLY);
    replaceLedgerByAPreviousBillingPeriodGrant();
    userSubscriptionProductService.endActiveSubscriptionProducts(
        JOE_DOE_ID, temporalUtils.startOfMonth());

    grantRequestedService.accept(
        MonthlySubscriptionCreditGrantRequested.builder().userId(JOE_DOE_ID).build());

    assertEquals(1, creditTransactionRepository.findAllByUserId(JOE_DOE_ID).size());
    assertEquals(0L, creditService.getCreditBalance(JOE_DOE_ID).getGrantedCredits());
  }

  @Test
  void resubscribing_during_the_cancelled_period_keeps_serving_the_cancelled_plan() {
    var nextPeriodStart = Instant.now().plus(20, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MICROS);
    userSubscriptionProductService.ensureActiveSubscriptionProduct(
        JOE_DOE_ID, ESSENTIAL_PLAN_ID, MONTHLY);
    userSubscriptionProductService.endActiveSubscriptionProducts(JOE_DOE_ID, nextPeriodStart);

    userSubscriptionProductService.ensureActiveSubscriptionProduct(
        JOE_DOE_ID, PRO_PLAN_ID, MONTHLY, nextPeriodStart);

    var servedProducts =
        userSubscriptionProductJpaRepository.findAllActiveByUserId(JOE_DOE_ID, Instant.now());
    assertEquals(1, servedProducts.size());
    assertEquals(ESSENTIAL_PLAN_ID, servedProducts.getFirst().getSubscriptionProduct().getId());
    assertEquals(nextPeriodStart, servedProducts.getFirst().getSubscriptionEndDatetime());
    var servedOnceStarted =
        userSubscriptionProductJpaRepository.findAllActiveByUserId(
            JOE_DOE_ID, nextPeriodStart.plusSeconds(1));
    assertEquals(1, servedOnceStarted.size());
    assertEquals(PRO_PLAN_ID, servedOnceStarted.getFirst().getSubscriptionProduct().getId());
  }

  @Test
  void resubscribing_during_the_cancelled_period_defers_the_new_plan_credits() {
    var nextPeriodStart = Instant.now().plus(20, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MICROS);
    userSubscriptionProductService.ensureActiveSubscriptionProduct(
        JOE_DOE_ID, ESSENTIAL_PLAN_ID, MONTHLY);
    userSubscriptionProductService.endActiveSubscriptionProducts(JOE_DOE_ID, nextPeriodStart);

    userSubscriptionProductService.ensureActiveSubscriptionProduct(
        JOE_DOE_ID, PRO_PLAN_ID, MONTHLY, nextPeriodStart);

    assertEquals(1, creditTransactionRepository.findAllByUserId(JOE_DOE_ID).size());
    assertEquals(
        ESSENTIAL_INCLUDED_CREDITS, creditService.getCreditBalance(JOE_DOE_ID).getGrantedCredits());
  }

  @Test
  void monthly_trigger_skips_a_user_whose_only_plan_has_not_started_yet() {
    var nextPeriodStart = Instant.now().plus(20, ChronoUnit.DAYS).truncatedTo(ChronoUnit.MICROS);
    userSubscriptionProductService.ensureActiveSubscriptionProduct(
        JOE_DOE_ID, ESSENTIAL_PLAN_ID, MONTHLY);
    userSubscriptionProductService.endActiveSubscriptionProducts(JOE_DOE_ID, Instant.now());
    userSubscriptionProductService.ensureActiveSubscriptionProduct(
        JOE_DOE_ID, PRO_PLAN_ID, MONTHLY, nextPeriodStart);

    grantTriggeredService.accept(new MonthlySubscriptionCreditGrantTriggered());

    verify(eventProducer, never()).accept(anyList());
  }
}
