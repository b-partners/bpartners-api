package app.bpartners.api.unit.service;

import static app.bpartners.api.model.activity.UserActivityType.ROOF_DETECTION_2D;
import static app.bpartners.api.model.activity.UserActivityUnit.UNIT;
import static app.bpartners.api.model.project.AnalysisProjectStep.ANALYSIS_2D;
import static java.time.Instant.now;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.api.model.User;
import app.bpartners.api.model.activity.UserActivity;
import app.bpartners.api.model.detection.DetectionInitiator;
import app.bpartners.api.model.detection.DetectionTracking;
import app.bpartners.api.model.exception.InsufficientCreditsException;
import app.bpartners.api.repository.DetectionTrackingRepository;
import app.bpartners.api.service.activity.UserActivityService;
import app.bpartners.api.service.detection.DetectionTrackingService;
import app.bpartners.api.service.project.AnalysisProjectService;
import app.bpartners.api.service.utils.CustomDateFormatter;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class DetectionTrackingServiceTest {
  DetectionTrackingRepository repositoryMock = mock();
  UserActivityService userActivityServiceMock = mock();
  AnalysisProjectService analysisProjectServiceMock = mock();

  DetectionTrackingService subject =
      new DetectionTrackingService(
          repositoryMock,
          userActivityServiceMock,
          new CustomDateFormatter(),
          analysisProjectServiceMock);

  @Test
  void save_tracking_adds_consumption_log_and_debits_credits() {
    var userId = randomUUID().toString();
    var tracking = List.of(someTracking(userId));
    when(repositoryMock.saveAll(tracking)).thenReturn(tracking);
    var userActivityCaptor = ArgumentCaptor.forClass(UserActivity.class);

    var actual = subject.computeTrackingWithUserActivity(tracking);

    assertEquals(tracking, actual);
    verify(userActivityServiceMock, times(1)).record(userActivityCaptor.capture());
    var userActivity = userActivityCaptor.getValue();
    assertEquals(ROOF_DETECTION_2D, userActivity.getActivityType());
    assertEquals(UNIT, userActivity.getActivityUnit());
    assertEquals(1L, userActivity.getQuantity());
    assertEquals(userId, userActivity.getUserId());
    verify(analysisProjectServiceMock, times(1))
        .register(eq(userId), anyString(), any(), eq(ANALYSIS_2D), anyString(), any());
  }

  @Test
  void debit_label_mentions_the_analysed_address() {
    var userId = randomUUID().toString();
    var tracking = List.of(someTracking(userId));
    when(repositoryMock.saveAll(tracking)).thenReturn(tracking);
    var labelCaptor = ArgumentCaptor.forClass(String.class);

    subject.computeTrackingWithUserActivity(tracking);

    verify(analysisProjectServiceMock)
        .register(eq(userId), anyString(), any(), eq(ANALYSIS_2D), labelCaptor.capture(), any());
    assertEquals("Analyse toiture : some zone", labelCaptor.getValue());
  }

  @Test
  void debit_once_per_saved_tracking_for_every_user() {
    var firstUserId = randomUUID().toString();
    var secondUserId = randomUUID().toString();
    var tracking =
        List.of(someTracking(firstUserId), someTracking(firstUserId), someTracking(secondUserId));
    when(repositoryMock.saveAll(tracking)).thenReturn(tracking);

    subject.computeTrackingWithUserActivity(tracking);

    verify(userActivityServiceMock, times(3)).record(any());
    verify(analysisProjectServiceMock, times(2))
        .register(eq(firstUserId), anyString(), any(), eq(ANALYSIS_2D), anyString(), any());
    verify(analysisProjectServiceMock, times(1))
        .register(eq(secondUserId), anyString(), any(), eq(ANALYSIS_2D), anyString(), any());
  }

  @Test
  void insufficient_credits_is_not_swallowed_when_debiting() {
    var userId = randomUUID().toString();
    var tracking = List.of(someTracking(userId));
    when(repositoryMock.saveAll(tracking)).thenReturn(tracking);
    when(analysisProjectServiceMock.register(
            eq(userId), anyString(), any(), eq(ANALYSIS_2D), anyString(), any()))
        .thenThrow(new InsufficientCreditsException(1L, 0L));

    assertThrows(
        InsufficientCreditsException.class,
        () -> subject.computeTrackingWithUserActivity(tracking));

    verify(userActivityServiceMock, times(1)).record(any());
  }

  @Test
  void save_tracking_without_detection_identifier() {
    var userId = randomUUID().toString();
    var tracking = List.of(someTracking(userId, null));
    when(repositoryMock.saveAll(tracking)).thenReturn(tracking);

    var actual = subject.computeTrackingWithUserActivity(tracking);

    assertEquals(tracking, actual);
    verify(repositoryMock, never()).findByDetectionIdentifier(any());
    verify(userActivityServiceMock, times(1)).record(any());
    verify(analysisProjectServiceMock, times(1))
        .register(eq(userId), anyString(), any(), eq(ANALYSIS_2D), anyString(), any());
  }

  @Test
  void skip_detection_already_registered_with_the_same_identifier() {
    var userId = randomUUID().toString();
    var detectionIdentifier = randomUUID().toString();
    var alreadyRegistered = someTracking(userId, detectionIdentifier);
    when(repositoryMock.findByDetectionIdentifier(detectionIdentifier))
        .thenReturn(Optional.of(alreadyRegistered));

    var actual =
        subject.computeTrackingWithUserActivity(List.of(someTracking(userId, detectionIdentifier)));

    assertTrue(actual.isEmpty());
    verify(repositoryMock).saveAll(List.of());
    verify(userActivityServiceMock, never()).record(any());
    verify(analysisProjectServiceMock, never()).register(any(), any(), any(), any(), any(), any());
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

    var actual = subject.computeTrackingWithUserActivity(List.of(known, unknown));

    assertEquals(List.of(unknown), actual);
    verify(repositoryMock).saveAll(List.of(unknown));
    verify(userActivityServiceMock, times(1)).record(any());
    verify(analysisProjectServiceMock, times(1))
        .register(eq(userId), anyString(), any(), eq(ANALYSIS_2D), anyString(), any());
  }

  @Test
  void skip_detection_whose_zone_is_already_registered() {
    var userId = randomUUID().toString();
    var detectionIdentifier = randomUUID().toString();
    when(repositoryMock.findByDetectionIdentifier(detectionIdentifier))
        .thenReturn(Optional.empty());
    when(repositoryMock.existsByIdUserAndZoneIgnoreCase(userId, "some zone")).thenReturn(true);

    var actual =
        subject.computeTrackingWithUserActivity(List.of(someTracking(userId, detectionIdentifier)));

    assertTrue(actual.isEmpty());
    verify(repositoryMock).saveAll(List.of());
    verify(userActivityServiceMock, never()).record(any());
    verify(analysisProjectServiceMock, never()).register(any(), any(), any(), any(), any(), any());
  }

  @Test
  void skip_detection_with_known_zone_even_without_detection_identifier() {
    var userId = randomUUID().toString();
    when(repositoryMock.existsByIdUserAndZoneIgnoreCase(userId, "some zone")).thenReturn(true);

    var actual = subject.computeTrackingWithUserActivity(List.of(someTracking(userId, null)));

    assertTrue(actual.isEmpty());
    verify(userActivityServiceMock, never()).record(any());
    verify(analysisProjectServiceMock, never()).register(any(), any(), any(), any(), any(), any());
  }

  @Test
  void zone_registered_by_another_user_does_not_skip_the_detection() {
    var userId = randomUUID().toString();
    var otherUserId = randomUUID().toString();
    var tracking = someTracking(userId, null);
    when(repositoryMock.existsByIdUserAndZoneIgnoreCase(otherUserId, "some zone")).thenReturn(true);
    when(repositoryMock.existsByIdUserAndZoneIgnoreCase(userId, "some zone")).thenReturn(false);
    when(repositoryMock.saveAll(List.of(tracking))).thenReturn(List.of(tracking));

    var actual = subject.computeTrackingWithUserActivity(List.of(tracking));

    assertEquals(List.of(tracking), actual);
    verify(userActivityServiceMock, times(1)).record(any());
    verify(analysisProjectServiceMock, times(1))
        .register(eq(userId), anyString(), any(), eq(ANALYSIS_2D), anyString(), any());
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

    var actual = subject.computeTrackingWithUserActivity(List.of(first, second));

    assertEquals(List.of(first), actual);
    verify(userActivityServiceMock, times(1)).record(any());
    verify(analysisProjectServiceMock, times(1))
        .register(eq(userId), anyString(), any(), eq(ANALYSIS_2D), anyString(), any());
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
        detectionIdentifier,
        null,
        null);
  }
}
