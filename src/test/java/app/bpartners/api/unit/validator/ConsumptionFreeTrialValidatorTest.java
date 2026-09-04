package app.bpartners.api.unit.validator;

import static app.bpartners.api.model.WhiteListScope.API_KEY_NOT_RESTRICTED_BY_TRIAL;
import static app.bpartners.api.model.subscription.SubscriptionConsumptionType.ROOF_ANALYSIS;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import app.bpartners.api.model.UserWhiteListed;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.subscription.SubscriptionConsumptionLog;
import app.bpartners.api.model.subscription.SubscriptionConsumptionType;
import app.bpartners.api.model.subscription.UserSubscriptionEligible;
import app.bpartners.api.repository.jpa.SubscriptionConsumptionLogJpaRepository;
import app.bpartners.api.repository.jpa.UserWhiteListedJpaRepository;
import app.bpartners.api.service.subscription.RoofAnalysisConsumptionFreeTrialValidator;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ConsumptionFreeTrialValidatorTest {
  SubscriptionConsumptionLogJpaRepository consumptionLogJpaRepositoryMock = mock();
  UserWhiteListedJpaRepository userWhiteListedJpaRepositoryMock = mock();

  RoofAnalysisConsumptionFreeTrialValidator subject =
      new RoofAnalysisConsumptionFreeTrialValidator(
          consumptionLogJpaRepositoryMock, userWhiteListedJpaRepositoryMock);

  @Test
  void consumption_under_max_free_consumption_ok() {
    var userId = randomUUID().toString();
    var eligible = someEligible(userId, true);
    givenConsumption(userId, ROOF_ANALYSIS, 19);

    assertDoesNotThrow(() -> subject.accept(eligible));

    verify(userWhiteListedJpaRepositoryMock, never()).findByUserId(any());
  }

  @Test
  void consumption_equals_max_free_consumption_ko() {
    var userId = randomUUID().toString();
    var eligible = someEligible(userId, true);
    givenConsumption(userId, ROOF_ANALYSIS, 20);
    when(userWhiteListedJpaRepositoryMock.findByUserId(userId)).thenReturn(Optional.empty());

    var actual = assertThrows(BadRequestException.class, () -> subject.accept(eligible));

    assertEquals(
        "Roof analysis consumption 20 limit exceeded for free trial period for User.id=" + userId,
        actual.getMessage());
  }

  @Test
  void consumption_over_max_free_consumption_with_white_listed_user_ok() {
    var userId = randomUUID().toString();
    var eligible = someEligible(userId, true);
    var userWhiteListedMock = mock(UserWhiteListed.class);
    givenConsumption(userId, ROOF_ANALYSIS, 42);
    when(userWhiteListedMock.getScopes()).thenReturn(List.of(API_KEY_NOT_RESTRICTED_BY_TRIAL));
    when(userWhiteListedJpaRepositoryMock.findByUserId(userId))
        .thenReturn(Optional.of(userWhiteListedMock));

    assertDoesNotThrow(() -> subject.accept(eligible));
  }

  @Test
  void consumption_over_max_free_consumption_with_subscribed_user_ok() {
    var userId = randomUUID().toString();
    var eligible = someEligible(userId, false);
    givenConsumption(userId, ROOF_ANALYSIS, 42);

    assertDoesNotThrow(() -> subject.accept(eligible));

    verify(userWhiteListedJpaRepositoryMock, never()).findByUserId(any());
    verify(consumptionLogJpaRepositoryMock, never())
        .findAllByUserIdAndConsumptionTypeAndCreationDatetimeBetween(any(), any(), any(), any());
  }

  private void givenConsumption(String userId, SubscriptionConsumptionType type, int nb) {
    when(consumptionLogJpaRepositoryMock
            .findAllByUserIdAndConsumptionTypeAndCreationDatetimeBetween(
                eq(userId), eq(type), any(Instant.class), any(Instant.class)))
        .thenReturn(someConsumptionLogs(nb, type));
  }

  private UserSubscriptionEligible someEligible(String userId, boolean freeTrialPeriodActive) {
    var userSubscriptionEligibleMock = mock(UserSubscriptionEligible.class);
    when(userSubscriptionEligibleMock.getUserId()).thenReturn(userId);
    when(userSubscriptionEligibleMock.getEligibleFrom()).thenReturn(LocalDate.now());
    when(userSubscriptionEligibleMock.hasFreeTrialPeriodActive()).thenReturn(freeTrialPeriodActive);
    return userSubscriptionEligibleMock;
  }

  private List<SubscriptionConsumptionLog> someConsumptionLogs(
      int nb, SubscriptionConsumptionType type) {
    List<SubscriptionConsumptionLog> subscriptionConsumptionLogs = new ArrayList<>();
    for (int i = 0; i < nb; i++) {
      subscriptionConsumptionLogs.add(
          SubscriptionConsumptionLog.builder().consumptionType(type).usageMetric(1L).build());
    }
    return subscriptionConsumptionLogs;
  }
}
