package app.bpartners.api.unit.service;

import static app.bpartners.api.model.subscription.SubscriptionConsumptionType.ROOF_ANALYSIS;
import static app.bpartners.api.model.subscription.SubscriptionConsumptionUnit.UNIT;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.api.model.User;
import app.bpartners.api.model.detection.DetectionInitiator;
import app.bpartners.api.model.detection.DetectionTracking;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.subscription.SubscriptionConsumptionLog;
import app.bpartners.api.model.subscription.UserSubscriptionEligible;
import app.bpartners.api.repository.DetectionTrackingRepository;
import app.bpartners.api.repository.jpa.UserSubscriptionEligibleJpaRepository;
import app.bpartners.api.service.detection.DetectionTrackingService;
import app.bpartners.api.service.subscription.RoofAnalysisConsumptionFreeTrialValidator;
import app.bpartners.api.service.subscription.SubscriptionService;
import app.bpartners.api.service.utils.CustomDateFormatter;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DetectionTrackingServiceTest {
  DetectionTrackingRepository repositoryMock = mock();
  SubscriptionService subscriptionServiceMock = mock();
  UserSubscriptionEligibleJpaRepository subscriptionEligibleRepositoryMock = mock();
  RoofAnalysisConsumptionFreeTrialValidator roofAnalysisFreeTrialValidatorMock = mock();

  DetectionTrackingService subject =
      new DetectionTrackingService(
          repositoryMock,
          subscriptionServiceMock,
          new CustomDateFormatter(),
          subscriptionEligibleRepositoryMock,
          roofAnalysisFreeTrialValidatorMock);

  @Test
  void save_tracking_and_add_roof_analysis_consumption_log() {
    var userId = randomUUID().toString();
    var tracking = List.of(someTracking(userId));
    when(subscriptionEligibleRepositoryMock.findByUserId(userId))
        .thenReturn(Optional.of(mock(UserSubscriptionEligible.class)));
    when(repositoryMock.saveAll(tracking)).thenReturn(tracking);
    var consumptionLogCaptor = ArgumentCaptor.forClass(SubscriptionConsumptionLog.class);

    var actual = subject.computeTrackingWithSubscriptionConsumptionLog(tracking);

    assertEquals(tracking, actual);
    verify(subscriptionServiceMock, times(1)).addConsumption(consumptionLogCaptor.capture());
    var consumptionLog = consumptionLogCaptor.getValue();
    assertEquals(ROOF_ANALYSIS, consumptionLog.getConsumptionType());
    assertEquals(UNIT, consumptionLog.getConsumptionUnit());
    assertEquals(1L, consumptionLog.getUsageMetric());
    assertEquals(userId, consumptionLog.getUserId());
  }

  @Test
  void validate_free_trial_consumption_once_per_user_before_saving() {
    var firstUserId = randomUUID().toString();
    var secondUserId = randomUUID().toString();
    var tracking =
        List.of(someTracking(firstUserId), someTracking(firstUserId), someTracking(secondUserId));
    var firstEligible = mock(UserSubscriptionEligible.class);
    var secondEligible = mock(UserSubscriptionEligible.class);
    when(subscriptionEligibleRepositoryMock.findByUserId(firstUserId))
        .thenReturn(Optional.of(firstEligible));
    when(subscriptionEligibleRepositoryMock.findByUserId(secondUserId))
        .thenReturn(Optional.of(secondEligible));
    when(repositoryMock.saveAll(tracking)).thenReturn(tracking);

    subject.computeTrackingWithSubscriptionConsumptionLog(tracking);

    verify(roofAnalysisFreeTrialValidatorMock, times(1)).accept(firstEligible);
    verify(roofAnalysisFreeTrialValidatorMock, times(1)).accept(secondEligible);
  }

  @Test
  void do_not_save_anything_when_free_trial_consumption_exceeded() {
    var userId = randomUUID().toString();
    var tracking = List.of(someTracking(userId));
    var eligible = mock(UserSubscriptionEligible.class);
    when(subscriptionEligibleRepositoryMock.findByUserId(userId)).thenReturn(Optional.of(eligible));
    doThrow(new BadRequestException("Roof analysis consumption 20 limit exceeded"))
        .when(roofAnalysisFreeTrialValidatorMock)
        .accept(eligible);

    assertThrows(
        BadRequestException.class,
        () -> subject.computeTrackingWithSubscriptionConsumptionLog(tracking));

    verify(repositoryMock, never()).saveAll(any());
    verify(subscriptionServiceMock, never()).addConsumption(any());
  }

  @Test
  void do_not_validate_free_trial_consumption_when_user_not_eligible() {
    var userId = randomUUID().toString();
    var tracking = List.of(someTracking(userId));
    when(subscriptionEligibleRepositoryMock.findByUserId(userId)).thenReturn(Optional.empty());
    when(repositoryMock.saveAll(tracking)).thenReturn(tracking);

    assertDoesNotThrow(() -> subject.computeTrackingWithSubscriptionConsumptionLog(tracking));

    verify(roofAnalysisFreeTrialValidatorMock, never()).accept(any());
    verify(subscriptionServiceMock, times(1)).addConsumption(any());
  }

  private DetectionTracking someTracking(String userId) {
    return new DetectionTracking(
        randomUUID().toString(),
        "some zone",
        "some address",
        now(),
        new DetectionInitiator("some name", "some@email.com", "0600000000"),
        User.builder().id(userId).build());
  }
}
