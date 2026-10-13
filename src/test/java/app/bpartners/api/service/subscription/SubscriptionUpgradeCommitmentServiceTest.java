package app.bpartners.api.service.subscription;

import static app.bpartners.api.endpoint.rest.model.EnableStatus.DISABLED;
import static app.bpartners.api.endpoint.rest.model.EnableStatus.ENABLED;
import static app.bpartners.api.endpoint.rest.model.UserSubscriptionCommitmentDuration.TWELVE_MONTHS;
import static app.bpartners.api.model.subscription.BillingInterval.YEARLY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.model.UserSubscriptionCommitment;
import app.bpartners.api.model.UserSubscriptionCommitmentAutoRenewalStatusHistory;
import app.bpartners.api.repository.UserSubscriptionCommitmentAutoRenewalStatusHistoryJpaRepository;
import app.bpartners.api.repository.UserSubscriptionCommitmentJpaRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SubscriptionUpgradeCommitmentServiceTest {
  private static final String USER_ID = "user_id";
  private static final Instant START = Instant.parse("2026-10-11T22:00:00Z");
  private static final Instant END = Instant.parse("2027-10-11T22:00:00Z");

  UserSubscriptionCommitmentJpaRepository commitmentRepository = mock();
  UserSubscriptionCommitmentAutoRenewalStatusHistoryJpaRepository historyRepository = mock();
  UserSubscriptionProductService userSubscriptionProductService = mock();
  SubscriptionUpgradeCommitmentService subject =
      new SubscriptionUpgradeCommitmentService(
          commitmentRepository, historyRepository, userSubscriptionProductService);

  @BeforeEach
  void setUp() {
    when(commitmentRepository.save(any(UserSubscriptionCommitment.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
  }

  @Test
  void finds_no_active_commitment_without_commitment() {
    when(commitmentRepository.findAllByUserId(USER_ID)).thenReturn(List.of());

    assertTrue(subject.findActiveCommitment(USER_ID).isEmpty());
  }

  @Test
  void ignores_expired_commitments_and_picks_the_most_recent_one() {
    var expired = commitment("expired", "2026-01-01T00:00:00Z", "2026-01-01T00:00:00Z", END, true);
    var older = commitment("older", "2025-01-01T00:00:00Z", "2025-01-01T00:00:00Z", END, false);
    var newer = commitment("newer", "2026-01-01T00:00:00Z", "2026-01-01T00:00:00Z", END, false);
    when(commitmentRepository.findAllByUserId(USER_ID)).thenReturn(List.of(expired, older, newer));

    assertEquals("newer", subject.findActiveCommitment(USER_ID).orElseThrow().getId());
  }

  @Test
  void breaks_a_tie_on_the_start_with_the_creation_datetime() {
    var first = commitment("first", "2026-01-01T00:00:00Z", "2026-01-02T00:00:00Z", END, false);
    var second = commitment("second", "2026-01-01T00:00:00Z", "2026-01-03T00:00:00Z", END, false);
    when(commitmentRepository.findAllByUserId(USER_ID)).thenReturn(List.of(second, first));

    assertEquals("second", subject.findActiveCommitment(USER_ID).orElseThrow().getId());
  }

  @Test
  void treats_missing_start_and_creation_datetimes_as_the_oldest() {
    var undated = commitment("undated", null, null, END, false);
    var dated = commitment("dated", "2026-01-01T00:00:00Z", "2026-01-01T00:00:00Z", END, false);
    when(commitmentRepository.findAllByUserId(USER_ID)).thenReturn(List.of(dated, undated));

    assertEquals("dated", subject.findActiveCommitment(USER_ID).orElseThrow().getId());
  }

  @Test
  void orders_commitments_having_no_start_by_creation_datetime() {
    var first = commitment("first", null, "2026-01-02T00:00:00Z", END, false);
    var second = commitment("second", null, null, END, false);
    when(commitmentRepository.findAllByUserId(USER_ID)).thenReturn(List.of(second, first));

    assertEquals("first", subject.findActiveCommitment(USER_ID).orElseThrow().getId());
  }

  @Test
  void switches_expiring_the_previous_commitment_and_carrying_its_renewal_status() {
    var previous =
        commitment("previous", "2026-01-01T00:00:00Z", "2026-01-01T00:00:00Z", END, false)
            .toBuilder()
            .autoRenewalStatusHistory(
                List.of(
                    history("h1", ENABLED, "2026-01-01T00:00:00Z"),
                    history("h2", DISABLED, "2026-02-01T00:00:00Z")))
            .build();
    when(commitmentRepository.findAllByUserId(USER_ID)).thenReturn(List.of(previous));

    var renewed = subject.switchTo(USER_ID, "pro_id", YEARLY, START, END);

    var saved = ArgumentCaptor.forClass(UserSubscriptionCommitment.class);
    verify(commitmentRepository, times(2)).save(saved.capture());
    var expired = saved.getAllValues().get(0);
    assertEquals("previous", expired.getId());
    assertNotNull(expired.getExpiredDatetime());
    assertEquals(START, expired.getCommitmentEndDatetime());
    assertEquals("pro_id", renewed.getSubscriptionPlanIdentifier());
    assertEquals(TWELVE_MONTHS, renewed.getDuration());
    assertEquals(START, renewed.getCommitmentStartDatetime());
    assertEquals(END, renewed.getCommitmentEndDatetime());
    assertNull(renewed.getExpiredDatetime());
    var carried = ArgumentCaptor.forClass(UserSubscriptionCommitmentAutoRenewalStatusHistory.class);
    verify(historyRepository).save(carried.capture());
    assertEquals(DISABLED, carried.getValue().getAutoRenewalStatus());
    assertEquals(renewed.getId(), carried.getValue().getUserSubscriptionCommitmentId());
    verify(userSubscriptionProductService).endActiveSubscriptionProducts(any(), any());
    verify(userSubscriptionProductService)
        .ensureActiveSubscriptionProduct(any(), any(), any(), any());
  }

  @Test
  void keeps_the_end_of_a_previous_commitment_that_already_ends_before_the_switch() {
    var endingBefore = Instant.parse("2026-10-01T00:00:00Z");
    var previous =
        commitment("previous", "2025-10-01T00:00:00Z", "2025-10-01T00:00:00Z", endingBefore, false);
    when(commitmentRepository.findAllByUserId(USER_ID)).thenReturn(List.of(previous));

    subject.switchTo(USER_ID, "pro_id", YEARLY, START, END);

    var saved = ArgumentCaptor.forClass(UserSubscriptionCommitment.class);
    verify(commitmentRepository, times(2)).save(saved.capture());
    assertEquals(endingBefore, saved.getAllValues().get(0).getCommitmentEndDatetime());
    verify(historyRepository, never()).save(any());
  }

  @Test
  void ends_a_previous_commitment_without_end_at_the_switch() {
    var previous =
        commitment("previous", "2025-10-01T00:00:00Z", "2025-10-01T00:00:00Z", null, false);
    when(commitmentRepository.findAllByUserId(USER_ID)).thenReturn(List.of(previous));

    subject.switchTo(USER_ID, "pro_id", YEARLY, START, END);

    var saved = ArgumentCaptor.forClass(UserSubscriptionCommitment.class);
    verify(commitmentRepository, times(2)).save(saved.capture());
    assertEquals(START, saved.getAllValues().get(0).getCommitmentEndDatetime());
  }

  @Test
  void opens_a_renewed_commitment_without_previous_one() {
    when(commitmentRepository.findAllByUserId(USER_ID)).thenReturn(List.of());

    var renewed = subject.switchTo(USER_ID, "pro_id", YEARLY, START, END);

    verify(commitmentRepository, times(1)).save(any());
    verify(historyRepository, never()).save(any());
    assertEquals("pro_id", renewed.getSubscriptionPlanIdentifier());
  }

  private static UserSubscriptionCommitment commitment(
      String id, String start, String creation, Instant end, boolean expired) {
    return UserSubscriptionCommitment.builder()
        .id(id)
        .userId(USER_ID)
        .commitmentStartDatetime(start == null ? null : Instant.parse(start))
        .creationDatetime(creation == null ? null : Instant.parse(creation))
        .commitmentEndDatetime(end)
        .expiredDatetime(expired ? Instant.parse("2026-06-01T00:00:00Z") : null)
        .build();
  }

  private static UserSubscriptionCommitmentAutoRenewalStatusHistory history(
      String id, app.bpartners.api.endpoint.rest.model.EnableStatus status, String creation) {
    return UserSubscriptionCommitmentAutoRenewalStatusHistory.builder()
        .id(id)
        .autoRenewalStatus(status)
        .creationDatetime(Instant.parse(creation))
        .build();
  }
}
