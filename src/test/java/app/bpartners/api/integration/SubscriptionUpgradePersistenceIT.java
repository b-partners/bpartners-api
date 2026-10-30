package app.bpartners.api.integration;

import static app.bpartners.api.endpoint.rest.model.EnableStatus.DISABLED;
import static app.bpartners.api.endpoint.rest.model.UserSubscriptionCommitmentDuration.TWELVE_MONTHS;
import static app.bpartners.api.model.subscription.BillingInterval.MONTHLY;
import static app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.ABANDONED;
import static app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.AWAITING_PAYMENT;
import static app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.PAID;
import static app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.PENDING;
import static app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.PROCESSING;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.model.UserSubscriptionCommitment;
import app.bpartners.api.model.UserSubscriptionCommitmentAutoRenewalStatusHistory;
import app.bpartners.api.model.subscription.UpgradeLineKind;
import app.bpartners.api.model.subscription.UserSubscriptionUpgrade;
import app.bpartners.api.model.subscription.UserSubscriptionUpgradeLine;
import app.bpartners.api.repository.UserSubscriptionCommitmentAutoRenewalStatusHistoryJpaRepository;
import app.bpartners.api.repository.UserSubscriptionCommitmentJpaRepository;
import app.bpartners.api.repository.jpa.UserSubscriptionProductJpaRepository;
import app.bpartners.api.repository.jpa.UserSubscriptionUpgradeJpaRepository;
import app.bpartners.api.service.subscription.StripeInvoiceVoidOutcome;
import app.bpartners.api.service.subscription.SubscriptionUpgradeCommitmentService;
import app.bpartners.api.service.subscription.SubscriptionUpgradeTransitionService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

class SubscriptionUpgradePersistenceIT extends MockedThirdParties {
  private static final String JANE_DOE_ID = "jane_doe_id";
  private static final String SUBSCRIPTION_PRODUCT_ID = "aef20fa7-5d85-4900-bad5-ee121be69ef3";

  @Autowired private UserSubscriptionCommitmentJpaRepository commitmentRepository;

  @Autowired
  private UserSubscriptionCommitmentAutoRenewalStatusHistoryJpaRepository
      autoRenewalHistoryRepository;

  @Autowired private UserSubscriptionProductJpaRepository userSubscriptionProductRepository;
  @Autowired private UserSubscriptionUpgradeJpaRepository upgradeRepository;
  @Autowired private SubscriptionUpgradeCommitmentService commitmentService;
  @Autowired private SubscriptionUpgradeTransitionService transitionService;
  @PersistenceContext private EntityManager entityManager;

  @Test
  @Transactional
  void upgrade_round_trips_with_its_enums_and_status() {
    var upgrade =
        upgradeRepository.save(
            UserSubscriptionUpgrade.builder()
                .id(randomUUID().toString())
                .userId(JANE_DOE_ID)
                .fromSubscriptionProductId(SUBSCRIPTION_PRODUCT_ID)
                .toSubscriptionProductId(SUBSCRIPTION_PRODUCT_ID)
                .fromBillingInterval(MONTHLY)
                .toBillingInterval(app.bpartners.api.model.subscription.BillingInterval.YEARLY)
                .upgradeDatetime(now())
                .oldCoverageEndDatetime(now().plus(200, DAYS))
                .newCommitmentEndDatetime(now().plus(365, DAYS))
                .prorationAmountInCentsWithVat(12_345L)
                .stripeProrationInvoiceId("in_" + randomUUID())
                .status(PENDING)
                .creationDatetime(now())
                .build());
    upgradeRepository.save(upgrade.toBuilder().status(PAID).build());
    entityManager.flush();
    entityManager.clear();

    var actual = upgradeRepository.findById(upgrade.getId()).orElseThrow();

    assertEquals(PAID, actual.getStatus());
    assertEquals(
        List.of(PENDING, PAID),
        actual.getStatusHistory().stream().map(history -> history.getStatus()).toList());
    assertEquals(12_345L, actual.getProrationAmountInCentsWithVat());
    assertEquals(
        app.bpartners.api.model.subscription.BillingInterval.YEARLY, actual.getToBillingInterval());
    assertEquals(
        upgrade.getId(),
        upgradeRepository
            .findByStripeProrationInvoiceId(upgrade.getStripeProrationInvoiceId())
            .orElseThrow()
            .getId());
  }

  @Test
  @Transactional
  void awaiting_payment_upgrade_round_trips_its_payment_page_and_is_found_once_overdue() {
    var deadline = now().minus(1, HOURS).truncatedTo(SECONDS);
    var upgrade = upgradeRepository.save(awaitingUpgrade("in_" + randomUUID(), deadline));
    entityManager.flush();
    entityManager.clear();

    var actual = upgradeRepository.findById(upgrade.getId()).orElseThrow();

    assertEquals(AWAITING_PAYMENT, actual.getStatus());
    assertEquals("https://pay.example/" + upgrade.getId(), actual.getPaymentUrl());
    assertEquals(deadline, actual.getPaymentExpirationDatetime());
    assertEquals("sub_old", actual.getOldStripeSubscriptionId());
    assertTrue(
        upgradeRepository.findAllByPaymentExpirationDatetimeBefore(now()).stream()
            .anyMatch(overdue -> overdue.getId().equals(upgrade.getId())));
    assertFalse(
        upgradeRepository
            .findAllByPaymentExpirationDatetimeBefore(deadline.minusSeconds(1))
            .stream()
            .anyMatch(overdue -> overdue.getId().equals(upgrade.getId())));
  }

  @Test
  @Transactional
  void claiming_a_paid_upgrade_moves_it_to_processing_only_once() {
    var stripeInvoiceId = "in_" + randomUUID();
    var upgrade = upgradeRepository.save(awaitingUpgrade(stripeInvoiceId, now().plus(1, HOURS)));
    entityManager.flush();
    entityManager.clear();

    var claimed = transitionService.claimPaid(upgrade.getId(), stripeInvoiceId);
    entityManager.flush();
    entityManager.clear();
    var claimedAgain = transitionService.claimPaid(upgrade.getId(), stripeInvoiceId);

    assertEquals(PROCESSING, claimed.orElseThrow().getStatus());
    assertTrue(claimedAgain.isEmpty());
    assertEquals(
        List.of(PENDING, AWAITING_PAYMENT, PROCESSING),
        upgradeRepository.findById(upgrade.getId()).orElseThrow().getStatusHistory().stream()
            .map(history -> history.getStatus())
            .toList());
  }

  @Test
  @Transactional
  void abandoning_an_upgrade_awaiting_its_payment_persists_the_abandoned_status() {
    var stripeInvoiceId = "in_" + randomUUID();
    var upgrade = upgradeRepository.save(awaitingUpgrade(stripeInvoiceId, now().plus(1, HOURS)));
    when(stripeUpgradeServiceMock.voidOpenInvoice(stripeInvoiceId))
        .thenReturn(StripeInvoiceVoidOutcome.VOIDED);
    entityManager.flush();
    entityManager.clear();

    var abandonment = transitionService.abandon(upgrade.getId());
    entityManager.flush();
    entityManager.clear();

    assertEquals(SubscriptionUpgradeTransitionService.Abandonment.ABANDONED, abandonment);
    assertEquals(
        List.of(PENDING, AWAITING_PAYMENT, ABANDONED),
        upgradeRepository.findById(upgrade.getId()).orElseThrow().getStatusHistory().stream()
            .map(history -> history.getStatus())
            .toList());
  }

  @Test
  @Transactional
  void quote_round_trips_its_proration_detail_in_order() {
    var quote =
        upgradeRepository.save(
            awaitingUpgrade("in_" + randomUUID(), now().plus(1, HOURS)).toBuilder()
                .newChargeInCentsWithVat(7_742L)
                .creditedInCentsWithVat(3_871L)
                .paidPeriodStartDate(java.time.LocalDate.of(2026, 10, 1))
                .paidPeriodEndDate(java.time.LocalDate.of(2026, 10, 31))
                .paidAmountInCentsWithVat(6_000L)
                .quoteExpirationDatetime(now().plus(1, HOURS).truncatedTo(SECONDS))
                .lines(
                    List.of(
                        line(1, UpgradeLineKind.CREDIT, 3_871L),
                        line(0, UpgradeLineKind.NEW_CHARGE, 7_742L)))
                .build());
    entityManager.flush();
    entityManager.clear();

    var actual = upgradeRepository.findById(quote.getId()).orElseThrow();

    assertEquals(7_742L, actual.getNewChargeInCentsWithVat());
    assertEquals(3_871L, actual.getCreditedInCentsWithVat());
    assertEquals(6_000L, actual.getPaidAmountInCentsWithVat());
    assertEquals(java.time.LocalDate.of(2026, 10, 1), actual.getPaidPeriodStartDate());
    assertEquals(java.time.LocalDate.of(2026, 10, 31), actual.getPaidPeriodEndDate());
    assertNotNull(actual.getQuoteExpirationDatetime());
    assertEquals(
        List.of(UpgradeLineKind.NEW_CHARGE, UpgradeLineKind.CREDIT),
        actual.getLines().stream().map(UserSubscriptionUpgradeLine::getKind).toList());
    assertEquals(
        List.of(7_742L, 3_871L),
        actual.getLines().stream()
            .map(UserSubscriptionUpgradeLine::getAmountInCentsWithVat)
            .toList());
    assertEquals(
        List.of(PENDING, AWAITING_PAYMENT),
        actual.getStatusHistory().stream().map(history -> history.getStatus()).toList());
  }

  @Test
  @Transactional
  void confirming_a_quote_moves_it_to_processing_and_stamps_the_confirmation() {
    var quote =
        upgradeRepository.save(
            awaitingUpgrade("in_" + randomUUID(), now().plus(1, HOURS)).toBuilder()
                .quoteExpirationDatetime(now().plus(1, HOURS))
                .stripeProrationInvoiceId(null)
                .status(PENDING)
                .build());
    entityManager.flush();
    entityManager.clear();

    var confirmed = transitionService.confirm(quote.getId());
    entityManager.flush();
    entityManager.clear();

    assertNotNull(confirmed.orElseThrow().getConfirmationDatetime());
    assertEquals(PROCESSING, upgradeRepository.findById(quote.getId()).orElseThrow().getStatus());
  }

  @Test
  @Transactional
  void abandoning_a_quote_persists_the_abandoned_status_without_stripe() {
    var quote =
        upgradeRepository.save(
            awaitingUpgrade("in_" + randomUUID(), now().plus(1, HOURS)).toBuilder()
                .quoteExpirationDatetime(now().plus(1, HOURS))
                .stripeProrationInvoiceId(null)
                .status(PENDING)
                .build());
    entityManager.flush();
    entityManager.clear();

    var abandonment = transitionService.abandon(quote.getId());
    entityManager.flush();
    entityManager.clear();

    assertEquals(SubscriptionUpgradeTransitionService.Abandonment.ABANDONED, abandonment);
    assertEquals(ABANDONED, upgradeRepository.findById(quote.getId()).orElseThrow().getStatus());
  }

  @Test
  @Transactional
  void acquires_the_advisory_lock_of_an_upgrade() {
    assertEquals(1, upgradeRepository.acquireLock("any_upgrade_id"));
  }

  @Test
  @Transactional
  void switching_flags_the_previous_commitment_as_expired_and_opens_a_renewed_one() {
    var switchStart = now().truncatedTo(SECONDS);
    var previous =
        commitmentRepository.save(
            UserSubscriptionCommitment.builder()
                .id(randomUUID().toString())
                .userId(JANE_DOE_ID)
                .subscriptionPlanIdentifier(SUBSCRIPTION_PRODUCT_ID)
                .duration(TWELVE_MONTHS)
                .commitmentStartDatetime(switchStart.minus(100, DAYS))
                .commitmentEndDatetime(switchStart.plus(265, DAYS))
                .creationDatetime(switchStart.minus(100, DAYS))
                .build());
    autoRenewalHistoryRepository.save(
        UserSubscriptionCommitmentAutoRenewalStatusHistory.builder()
            .id(randomUUID().toString())
            .userSubscriptionCommitmentId(previous.getId())
            .autoRenewalStatus(DISABLED)
            .creationDatetime(now())
            .build());
    entityManager.flush();
    entityManager.clear();

    var renewed =
        commitmentService.switchTo(
            JANE_DOE_ID,
            SUBSCRIPTION_PRODUCT_ID,
            MONTHLY,
            switchStart,
            switchStart.plus(365, DAYS));
    entityManager.flush();
    entityManager.clear();

    var expired = commitmentRepository.findById(previous.getId()).orElseThrow();
    assertNotNull(expired.getExpiredDatetime());
    assertTrue(expired.isExpired());
    assertFalse(expired.getCommitmentEndDatetime().isAfter(switchStart));

    var reloaded = commitmentRepository.findById(renewed.getId()).orElseThrow();
    assertFalse(reloaded.isExpired());
    assertEquals(TWELVE_MONTHS, reloaded.getDuration());
    assertEquals(switchStart.plus(365, DAYS), reloaded.getCommitmentEndDatetime());
    assertEquals(DISABLED, reloaded.getAutomaticRenewalStatus());
    assertNotEquals(previous.getId(), reloaded.getId());
    assertEquals(
        reloaded.getId(),
        commitmentService.findActiveCommitment(JANE_DOE_ID).orElseThrow().getId());
    var activeProduct =
        userSubscriptionProductRepository.findAllNotCancelledByUserId(JANE_DOE_ID).getFirst();
    assertEquals(SUBSCRIPTION_PRODUCT_ID, activeProduct.getSubscriptionProduct().getId());
    assertEquals(MONTHLY, activeProduct.getBillingInterval());
  }

  private static UserSubscriptionUpgradeLine line(int order, UpgradeLineKind kind, long amount) {
    return UserSubscriptionUpgradeLine.builder()
        .id(randomUUID().toString())
        .lineOrder(order)
        .kind(kind)
        .subscriptionProductId(SUBSCRIPTION_PRODUCT_ID)
        .billingInterval(MONTHLY)
        .periodStartDate(java.time.LocalDate.of(2026, 10, 12))
        .periodEndDate(java.time.LocalDate.of(2026, 10, 31))
        .days(20)
        .referencePeriodDays(31)
        .referencePriceInCentsWithVat(12_000L)
        .amountInCentsWithVat(amount)
        .build();
  }

  private UserSubscriptionUpgrade awaitingUpgrade(String stripeInvoiceId, Instant deadline) {
    var id = randomUUID().toString();
    return UserSubscriptionUpgrade.builder()
        .id(id)
        .userId(JANE_DOE_ID)
        .fromSubscriptionProductId(SUBSCRIPTION_PRODUCT_ID)
        .toSubscriptionProductId(SUBSCRIPTION_PRODUCT_ID)
        .fromBillingInterval(MONTHLY)
        .toBillingInterval(MONTHLY)
        .upgradeDatetime(now())
        .oldCoverageEndDatetime(now().plus(20, DAYS))
        .newCommitmentEndDatetime(now().plus(365, DAYS))
        .prorationAmountInCentsWithVat(3_871L)
        .stripeProrationInvoiceId(stripeInvoiceId)
        .oldStripeSubscriptionId("sub_old")
        .paymentUrl("https://pay.example/" + id)
        .paymentExpirationDatetime(deadline)
        .status(PENDING)
        .status(AWAITING_PAYMENT)
        .creationDatetime(now())
        .build();
  }
}
