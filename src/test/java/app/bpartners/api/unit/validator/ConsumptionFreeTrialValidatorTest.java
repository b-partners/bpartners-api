package app.bpartners.api.unit.validator;

import static app.bpartners.api.model.WhiteListScope.API_KEY_NOT_RESTRICTED_BY_TRIAL;
import static app.bpartners.api.model.subscription.SubscriptionConsumptionType.IMAGE_ACCESS;
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
import app.bpartners.api.service.subscription.ConsumptionFreeTrialValidator;
import app.bpartners.api.service.subscription.ImageAccessConsumptionFreeTrialValidator;
import app.bpartners.api.service.subscription.RoofAnalysisConsumptionFreeTrialValidator;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ConsumptionFreeTrialValidatorTest {
  SubscriptionConsumptionLogJpaRepository consumptionLogJpaRepositoryMock = mock();
  UserWhiteListedJpaRepository userWhiteListedJpaRepositoryMock = mock();

  RoofAnalysisConsumptionFreeTrialValidator roofAnalysisSubject =
      new RoofAnalysisConsumptionFreeTrialValidator(
          consumptionLogJpaRepositoryMock, userWhiteListedJpaRepositoryMock);
  ImageAccessConsumptionFreeTrialValidator imageAccessSubject =
      new ImageAccessConsumptionFreeTrialValidator(
          consumptionLogJpaRepositoryMock, userWhiteListedJpaRepositoryMock);

  static Stream<Arguments> consumptionTypes() {
    return Stream.of(
        Arguments.of(ROOF_ANALYSIS, "Roof analysis"), Arguments.of(IMAGE_ACCESS, "Image access"));
  }

  @ParameterizedTest
  @MethodSource("consumptionTypes")
  void consumption_under_max_free_consumption_ok(SubscriptionConsumptionType type, String label) {
    var userId = randomUUID().toString();
    var eligible = someEligible(userId, true);
    givenConsumption(userId, type, 19);

    assertDoesNotThrow(() -> subjectOf(type).accept(eligible));

    verify(userWhiteListedJpaRepositoryMock, never()).findByUserId(any());
  }

  @ParameterizedTest
  @MethodSource("consumptionTypes")
  void consumption_equals_max_free_consumption_ko(SubscriptionConsumptionType type, String label) {
    var userId = randomUUID().toString();
    var eligible = someEligible(userId, true);
    givenConsumption(userId, type, 20);
    when(userWhiteListedJpaRepositoryMock.findByUserId(userId)).thenReturn(Optional.empty());

    var actual = assertThrows(BadRequestException.class, () -> subjectOf(type).accept(eligible));

    assertEquals(
        label + " consumption 20 limit exceeded for free trial period for User.id=" + userId,
        actual.getMessage());
  }

  @ParameterizedTest
  @MethodSource("consumptionTypes")
  void consumption_over_max_free_consumption_with_white_listed_user_ok(
      SubscriptionConsumptionType type, String label) {
    var userId = randomUUID().toString();
    var eligible = someEligible(userId, true);
    var userWhiteListedMock = mock(UserWhiteListed.class);
    givenConsumption(userId, type, 42);
    when(userWhiteListedMock.getScopes()).thenReturn(List.of(API_KEY_NOT_RESTRICTED_BY_TRIAL));
    when(userWhiteListedJpaRepositoryMock.findByUserId(userId))
        .thenReturn(Optional.of(userWhiteListedMock));

    assertDoesNotThrow(() -> subjectOf(type).accept(eligible));
  }

  @ParameterizedTest
  @MethodSource("consumptionTypes")
  void consumption_over_max_free_consumption_with_subscribed_user_ok(
      SubscriptionConsumptionType type, String label) {
    var userId = randomUUID().toString();
    var eligible = someEligible(userId, false);
    givenConsumption(userId, type, 42);

    assertDoesNotThrow(() -> subjectOf(type).accept(eligible));

    verify(userWhiteListedJpaRepositoryMock, never()).findByUserId(any());
    verify(consumptionLogJpaRepositoryMock, never())
        .findAllByUserIdAndConsumptionTypeAndCreationDatetimeBetween(any(), any(), any(), any());
  }

  @Test
  void image_access_and_roof_analysis_have_their_own_free_trial_quota() {
    var userId = randomUUID().toString();
    var eligible = someEligible(userId, true);
    givenConsumption(userId, IMAGE_ACCESS, 20);
    givenConsumption(userId, ROOF_ANALYSIS, 3);
    when(userWhiteListedJpaRepositoryMock.findByUserId(userId)).thenReturn(Optional.empty());

    assertThrows(BadRequestException.class, () -> imageAccessSubject.accept(eligible));
    assertDoesNotThrow(() -> roofAnalysisSubject.accept(eligible));
  }

  private ConsumptionFreeTrialValidator subjectOf(SubscriptionConsumptionType type) {
    return ROOF_ANALYSIS.equals(type) ? roofAnalysisSubject : imageAccessSubject;
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
