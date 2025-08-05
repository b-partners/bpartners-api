package app.bpartners.api.unit.validator;

import static app.bpartners.api.model.subscription.SubscriptionConsumptionType.ROOF_ANALYSIS;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import app.bpartners.api.model.User;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.subscription.SubscriptionConsumptionLog;
import app.bpartners.api.model.subscription.SubscriptionConsumptionType;
import app.bpartners.api.model.subscription.UserSubscriptionEligible;
import app.bpartners.api.repository.jpa.UserApiKeyFullAuthorizationJpaRepository;
import app.bpartners.api.repository.jpa.model.UserApiKeyFullAuthorization;
import app.bpartners.api.service.areapicture.RoofAnalysisConsumptionFreeTrialValidator;
import app.bpartners.api.service.subscription.SubscriptionService;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class RoofAnalysisConsumptionFreeTrialValidatorTest {
  SubscriptionService subscriptionServiceMock = mock();
  UserApiKeyFullAuthorizationJpaRepository apiKeyFullAuthorizationRepositoryMock = mock();
  RoofAnalysisConsumptionFreeTrialValidator subject =
      new RoofAnalysisConsumptionFreeTrialValidator(
          subscriptionServiceMock, apiKeyFullAuthorizationRepositoryMock);

  @Test
  void any_validation_for_user_without_free_trial_period() {
    var userId = randomUUID().toString();
    var today = LocalDate.now();
    var userSubscriptionEligibleMock = mock(UserSubscriptionEligible.class);
    when(userSubscriptionEligibleMock.getUserId()).thenReturn(userId);
    when(userSubscriptionEligibleMock.getEligibleFrom()).thenReturn(today);
    when(userSubscriptionEligibleMock.hasFreeTrialPeriodActive()).thenReturn(false);

    assertDoesNotThrow(() -> subject.accept(userSubscriptionEligibleMock));

    verify(subscriptionServiceMock, never()).findConsumptionLogsByUserId(any(), any(), any());
  }

  @Test
  void consumptions_less_than_max_free_consumption_ok() {
    var userId = randomUUID().toString();
    var today = LocalDate.now();
    var userSubscriptionEligibleMock = mock(UserSubscriptionEligible.class);
    when(userSubscriptionEligibleMock.getUserId()).thenReturn(userId);
    when(userSubscriptionEligibleMock.getEligibleFrom()).thenReturn(today);
    when(userSubscriptionEligibleMock.hasFreeTrialPeriodActive()).thenReturn(true);
    when(subscriptionServiceMock.findConsumptionLogsByUserId(
            eq(userId), any(Instant.class), any(Instant.class)))
        .thenReturn(someConsumptionLogs(9, ROOF_ANALYSIS));

    assertDoesNotThrow(() -> subject.accept(userSubscriptionEligibleMock));
  }

  @Test
  void consumptions_equals_max_free_consumption_ko() {
    var userId = randomUUID().toString();
    var today = LocalDate.now();
    var userSubscriptionEligibleMock = mock(UserSubscriptionEligible.class);

    when(apiKeyFullAuthorizationRepositoryMock.findByIdUser(userId)).thenReturn(Optional.empty());
    when(userSubscriptionEligibleMock.getUserId()).thenReturn(userId);
    when(userSubscriptionEligibleMock.getEligibleFrom()).thenReturn(today);
    when(userSubscriptionEligibleMock.hasFreeTrialPeriodActive()).thenReturn(true);
    when(subscriptionServiceMock.findConsumptionLogsByUserId(
            eq(userId), any(Instant.class), any(Instant.class)))
        .thenReturn(someConsumptionLogs(20, ROOF_ANALYSIS));
    var expectedMessage =
        "Roof analysis consumption 20 limit exceeded"
            + " for free trial period for User.id="
            + userId;

    var actual =
        assertThrows(BadRequestException.class, () -> subject.accept(userSubscriptionEligibleMock));

    assertEquals(expectedMessage, actual.getMessage());
  }

  @Test
  void consumptions_equals_max_free_consumption_with_user_api_ok() {
    var userId = randomUUID().toString();
    var today = LocalDate.now();
    var userSubscriptionEligibleMock = mock(UserSubscriptionEligible.class);
    var userMock = mock(User.class);

    when(userMock.getApiKey()).thenReturn(randomUUID().toString());
    when(apiKeyFullAuthorizationRepositoryMock.findByIdUser(userId))
        .thenReturn(Optional.of(mock(UserApiKeyFullAuthorization.class)));
    when(userSubscriptionEligibleMock.getUserId()).thenReturn(userId);
    when(userSubscriptionEligibleMock.getEligibleFrom()).thenReturn(today);
    when(userSubscriptionEligibleMock.hasFreeTrialPeriodActive()).thenReturn(true);
    when(subscriptionServiceMock.findConsumptionLogsByUserId(
            eq(userId), any(Instant.class), any(Instant.class)))
        .thenReturn(someConsumptionLogs(10, ROOF_ANALYSIS));

    assertDoesNotThrow(() -> subject.accept(userSubscriptionEligibleMock));
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
