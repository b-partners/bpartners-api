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
import app.bpartners.api.model.exception.InsufficientCreditsException;
import app.bpartners.api.model.subscription.SubscriptionConsumptionLog;
import app.bpartners.api.repository.DetectionTrackingRepository;
import app.bpartners.api.service.credit.CreditService;
import app.bpartners.api.service.detection.DetectionTrackingService;
import app.bpartners.api.service.subscription.SubscriptionService;
import app.bpartners.api.service.utils.CustomDateFormatter;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DetectionTrackingServiceTest {
  DetectionTrackingRepository repositoryMock = mock();
  SubscriptionService subscriptionServiceMock = mock();
  CreditService creditServiceMock = mock();

  DetectionTrackingService subject =
      new DetectionTrackingService(
          repositoryMock, subscriptionServiceMock, new CustomDateFormatter(), creditServiceMock);

  @Test
  void save_tracking_adds_consumption_log_and_debits_credits() {
    var userId = randomUUID().toString();
    var tracking = List.of(someTracking(userId));
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
    verify(creditServiceMock, times(1)).consumeRoofAnalysis(eq(userId), anyString());
  }

  @Test
  void debit_label_mentions_the_analysed_address() {
    var userId = randomUUID().toString();
    var tracking = List.of(someTracking(userId));
    when(repositoryMock.saveAll(tracking)).thenReturn(tracking);
    var labelCaptor = ArgumentCaptor.forClass(String.class);

    subject.computeTrackingWithSubscriptionConsumptionLog(tracking);

    verify(creditServiceMock).consumeRoofAnalysis(eq(userId), labelCaptor.capture());
    assertEquals("Analyse toiture : some address", labelCaptor.getValue());
  }

  @Test
  void debit_once_per_saved_tracking_for_every_user() {
    var firstUserId = randomUUID().toString();
    var secondUserId = randomUUID().toString();
    var tracking =
        List.of(someTracking(firstUserId), someTracking(firstUserId), someTracking(secondUserId));
    when(repositoryMock.saveAll(tracking)).thenReturn(tracking);

    subject.computeTrackingWithSubscriptionConsumptionLog(tracking);

    verify(subscriptionServiceMock, times(3)).addConsumption(any());
    verify(creditServiceMock, times(2)).consumeRoofAnalysis(eq(firstUserId), anyString());
    verify(creditServiceMock, times(1)).consumeRoofAnalysis(eq(secondUserId), anyString());
  }

  @Test
  void insufficient_credits_is_not_swallowed_when_debiting() {
    var userId = randomUUID().toString();
    var tracking = List.of(someTracking(userId));
    when(repositoryMock.saveAll(tracking)).thenReturn(tracking);
    when(creditServiceMock.consumeRoofAnalysis(eq(userId), anyString()))
        .thenThrow(new InsufficientCreditsException(1L, 0L));

    assertThrows(
        InsufficientCreditsException.class,
        () -> subject.computeTrackingWithSubscriptionConsumptionLog(tracking));

    verify(subscriptionServiceMock, times(1)).addConsumption(any());
  }

  @Test
  void save_tracking_without_detection_identifier() {
    var userId = randomUUID().toString();
    var tracking = List.of(someTracking(userId, null));
    when(repositoryMock.saveAll(tracking)).thenReturn(tracking);

    var actual = subject.computeTrackingWithSubscriptionConsumptionLog(tracking);

    assertEquals(tracking, actual);
    verify(repositoryMock, never()).findByDetectionIdentifier(any());
    verify(subscriptionServiceMock, times(1)).addConsumption(any());
    verify(creditServiceMock, times(1)).consumeRoofAnalysis(eq(userId), anyString());
  }

  @Test
  void skip_detection_already_registered_with_the_same_identifier() {
    var userId = randomUUID().toString();
    var detectionIdentifier = randomUUID().toString();
    var alreadyRegistered = someTracking(userId, detectionIdentifier);
    when(repositoryMock.findByDetectionIdentifier(detectionIdentifier))
        .thenReturn(Optional.of(alreadyRegistered));

    var actual =
        subject.computeTrackingWithSubscriptionConsumptionLog(
            List.of(someTracking(userId, detectionIdentifier)));

    assertTrue(actual.isEmpty());
    verify(repositoryMock).saveAll(List.of());
    verify(subscriptionServiceMock, never()).addConsumption(any());
    verify(creditServiceMock, never()).consumeRoofAnalysis(any(), any());
  }

  @Test
  void save_only_the_detections_not_registered_yet() {
    var userId = randomUUID().toString();
    var knownIdentifier = randomUUID().toString();
    var unknownIdentifier = randomUUID().toString();
    var known = someTracking(userId, knownIdentifier);
    var unknown = someTracking(userId, unknownIdentifier);
    when(repositoryMock.findByDetectionIdentifier(knownIdentifier))
        .thenReturn(Optional.of(someTracking(userId, knownIdentifier)));
    when(repositoryMock.findByDetectionIdentifier(unknownIdentifier)).thenReturn(Optional.empty());
    when(repositoryMock.saveAll(List.of(unknown))).thenReturn(List.of(unknown));

    var actual = subject.computeTrackingWithSubscriptionConsumptionLog(List.of(known, unknown));

    assertEquals(List.of(unknown), actual);
    verify(repositoryMock).saveAll(List.of(unknown));
    verify(subscriptionServiceMock, times(1)).addConsumption(any());
    verify(creditServiceMock, times(1)).consumeRoofAnalysis(eq(userId), anyString());
  }

  @Test
  void deduplicate_detections_sharing_the_same_identifier_inside_a_batch() {
    var userId = randomUUID().toString();
    var detectionIdentifier = randomUUID().toString();
    var first = someTracking(userId, detectionIdentifier);
    var second = someTracking(userId, detectionIdentifier);
    when(repositoryMock.findByDetectionIdentifier(detectionIdentifier))
        .thenReturn(Optional.empty());
    when(repositoryMock.saveAll(List.of(first))).thenReturn(List.of(first));

    var actual = subject.computeTrackingWithSubscriptionConsumptionLog(List.of(first, second));

    assertEquals(List.of(first), actual);
    verify(subscriptionServiceMock, times(1)).addConsumption(any());
    verify(creditServiceMock, times(1)).consumeRoofAnalysis(eq(userId), anyString());
  }

  private DetectionTracking someTracking(String userId) {
    return someTracking(userId, null);
  }

  private DetectionTracking someTracking(String userId, String detectionIdentifier) {
    return new DetectionTracking(
        randomUUID().toString(),
        "some zone",
        "some address",
        now(),
        new DetectionInitiator("some name", "some@email.com", "0600000000"),
        User.builder().id(userId).build(),
        detectionIdentifier);
  }
}
