package app.bpartners.api.integration;

import static app.bpartners.api.endpoint.rest.model.EnableStatus.DISABLED;
import static app.bpartners.api.endpoint.rest.model.EnableStatus.ENABLED;
import static app.bpartners.api.endpoint.rest.model.UserSubscriptionCommitmentDuration.TWELVE_MONTHS;
import static java.time.Instant.now;
import static java.time.temporal.ChronoUnit.HOURS;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.model.UserSubscriptionCommitment;
import app.bpartners.api.model.UserSubscriptionCommitmentAutoRenewalStatusHistory;
import app.bpartners.api.repository.UserSubscriptionCommitmentAutoRenewalStatusHistoryJpaRepository;
import app.bpartners.api.repository.UserSubscriptionCommitmentJpaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

class UserSubscriptionCommitmentIT extends MockedThirdParties {
  private static final String JANE_DOE_ID = "jane_doe_id";
  private static final String SUBSCRIPTION_PRODUCT_ID = "aef20fa7-5d85-4900-bad5-ee121be69ef3";

  @Autowired private UserSubscriptionCommitmentJpaRepository commitmentJpaRepository;

  @Autowired
  private UserSubscriptionCommitmentAutoRenewalStatusHistoryJpaRepository
      autoRenewalStatusHistoryJpaRepository;

  @PersistenceContext private EntityManager entityManager;

  private String persistCommitment() {
    return commitmentJpaRepository
        .save(
            UserSubscriptionCommitment.builder()
                .id(randomUUID().toString())
                .userId(JANE_DOE_ID)
                .subscriptionPlanIdentifier(SUBSCRIPTION_PRODUCT_ID)
                .duration(TWELVE_MONTHS)
                .creationDatetime(now())
                .build())
        .getId();
  }

  private void persistAutoRenewalStatus(
      String commitmentId,
      app.bpartners.api.endpoint.rest.model.EnableStatus status,
      int hoursAgo) {
    autoRenewalStatusHistoryJpaRepository.save(
        UserSubscriptionCommitmentAutoRenewalStatusHistory.builder()
            .id(randomUUID().toString())
            .userSubscriptionCommitmentId(commitmentId)
            .autoRenewalStatus(status)
            .creationDatetime(now().minus(hoursAgo, HOURS))
            .build());
  }

  @Test
  @Transactional
  void automatic_renewal_status_is_null_without_history() {
    var commitmentId = persistCommitment();

    var actual = commitmentJpaRepository.findById(commitmentId).orElseThrow();

    assertNull(actual.getAutomaticRenewalStatus());
    // regression: the commitment_duration enum value must round-trip through Postgres
    assertEquals(TWELVE_MONTHS, actual.getDuration());
  }

  @Test
  @Transactional
  void automatic_renewal_status_reflects_the_latest_history_entry() {
    var commitmentId = persistCommitment();

    persistAutoRenewalStatus(commitmentId, ENABLED, 2);
    persistAutoRenewalStatus(commitmentId, DISABLED, 1);
    persistAutoRenewalStatus(commitmentId, ENABLED, 0);

    entityManager.flush();
    entityManager.clear();

    var actual = commitmentJpaRepository.findById(commitmentId).orElseThrow();

    assertEquals(ENABLED, actual.getAutomaticRenewalStatus());
  }
}
