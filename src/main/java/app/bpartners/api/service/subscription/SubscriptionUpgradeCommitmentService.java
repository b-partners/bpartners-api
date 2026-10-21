package app.bpartners.api.service.subscription;

import static app.bpartners.api.endpoint.rest.model.UserSubscriptionCommitmentDuration.TWELVE_MONTHS;
import static java.time.Instant.now;
import static java.util.Comparator.comparing;
import static java.util.UUID.randomUUID;

import app.bpartners.api.endpoint.rest.model.EnableStatus;
import app.bpartners.api.model.UserSubscriptionCommitment;
import app.bpartners.api.model.UserSubscriptionCommitmentAutoRenewalStatusHistory;
import app.bpartners.api.model.subscription.BillingInterval;
import app.bpartners.api.repository.UserSubscriptionCommitmentAutoRenewalStatusHistoryJpaRepository;
import app.bpartners.api.repository.UserSubscriptionCommitmentJpaRepository;
import java.time.Instant;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionUpgradeCommitmentService {
  private final UserSubscriptionCommitmentJpaRepository commitmentRepository;
  private final UserSubscriptionCommitmentAutoRenewalStatusHistoryJpaRepository
      autoRenewalStatusHistoryRepository;
  private final UserSubscriptionProductService userSubscriptionProductService;

  public Optional<UserSubscriptionCommitment> findActiveCommitment(String userId) {
    return commitmentRepository.findAllByUserId(userId).stream()
        .filter(commitment -> !commitment.isExpired())
        .max(
            comparing(
                    (UserSubscriptionCommitment commitment) ->
                        commitment.getCommitmentStartDatetime() == null
                            ? Instant.EPOCH
                            : commitment.getCommitmentStartDatetime())
                .thenComparing(
                    commitment ->
                        commitment.getCreationDatetime() == null
                            ? Instant.EPOCH
                            : commitment.getCreationDatetime()));
  }

  @Transactional
  public UserSubscriptionCommitment switchTo(
      String userId,
      String toSubscriptionPlanIdentifier,
      BillingInterval toBillingInterval,
      Instant commitmentStart,
      Instant commitmentEnd) {
    var switchDatetime = now();
    var previousCommitment = findActiveCommitment(userId);
    var previousAutoRenewalStatus =
        previousCommitment.map(UserSubscriptionCommitment::getAutomaticRenewalStatus).orElse(null);
    previousCommitment.ifPresent(
        commitment ->
            commitmentRepository.save(
                commitment.toBuilder()
                    .expiredDatetime(switchDatetime)
                    .commitmentEndDatetime(endNotAfter(commitment, commitmentStart))
                    .build()));
    var renewedCommitment =
        commitmentRepository.save(
            UserSubscriptionCommitment.builder()
                .id(randomUUID().toString())
                .userId(userId)
                .subscriptionPlanIdentifier(toSubscriptionPlanIdentifier)
                .duration(TWELVE_MONTHS)
                .approvalDatetime(switchDatetime)
                .commitmentStartDatetime(commitmentStart)
                .commitmentEndDatetime(commitmentEnd)
                .creationDatetime(switchDatetime)
                .build());
    if (previousAutoRenewalStatus != null) {
      autoRenewalStatusHistoryRepository.save(
          autoRenewalStatusOf(
              renewedCommitment.getId(), previousAutoRenewalStatus, switchDatetime));
    }
    userSubscriptionProductService.endActiveSubscriptionProducts(userId, switchDatetime);
    userSubscriptionProductService.ensureActiveSubscriptionProduct(
        userId, toSubscriptionPlanIdentifier, toBillingInterval, switchDatetime);
    log.info(
        "User(id={}) commitment renewed on SubscriptionProduct(id={}) from {} to {}, previous"
            + " commitment {} flagged as expired",
        userId,
        toSubscriptionPlanIdentifier,
        commitmentStart,
        commitmentEnd,
        previousCommitment.map(UserSubscriptionCommitment::getId).orElse(null));
    return renewedCommitment;
  }

  private static Instant endNotAfter(UserSubscriptionCommitment commitment, Instant limit) {
    var end = commitment.getCommitmentEndDatetime();
    return end == null || end.isAfter(limit) ? limit : end;
  }

  private static UserSubscriptionCommitmentAutoRenewalStatusHistory autoRenewalStatusOf(
      String commitmentId, EnableStatus status, Instant creationDatetime) {
    return UserSubscriptionCommitmentAutoRenewalStatusHistory.builder()
        .id(randomUUID().toString())
        .userSubscriptionCommitmentId(commitmentId)
        .autoRenewalStatus(status)
        .creationDatetime(creationDatetime)
        .build();
  }
}
