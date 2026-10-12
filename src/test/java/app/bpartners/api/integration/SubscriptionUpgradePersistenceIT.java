package app.bpartners.api.integration;

import static app.bpartners.api.endpoint.rest.model.EnableStatus.DISABLED;
import static app.bpartners.api.endpoint.rest.model.UserSubscriptionCommitmentDuration.TWELVE_MONTHS;
import static app.bpartners.api.model.subscription.BillingInterval.MONTHLY;
import static app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.PAID;
import static app.bpartners.api.model.subscription.SubscriptionUpgradeStatus.PENDING;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.DAYS;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.model.UserSubscriptionCommitment;
import app.bpartners.api.model.UserSubscriptionCommitmentAutoRenewalStatusHistory;
import app.bpartners.api.model.subscription.UserSubscriptionUpgrade;
import app.bpartners.api.repository.UserSubscriptionCommitmentAutoRenewalStatusHistoryJpaRepository;
import app.bpartners.api.repository.UserSubscriptionCommitmentJpaRepository;
import app.bpartners.api.repository.jpa.UserSubscriptionProductJpaRepository;
import app.bpartners.api.repository.jpa.UserSubscriptionUpgradeJpaRepository;
import app.bpartners.api.service.subscription.SubscriptionUpgradeCommitmentService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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
  void switching_flags_the_previous_commitment_as_expired_and_opens_a_renewed_one() {
    var switchStart = now();
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
}
