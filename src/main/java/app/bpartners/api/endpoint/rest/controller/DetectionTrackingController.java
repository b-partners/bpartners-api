package app.bpartners.api.endpoint.rest.controller;

import app.bpartners.api.endpoint.rest.mapper.detection.DetectionTrackingRestMapper;
import app.bpartners.api.endpoint.rest.model.CreateDetectionTracking;
import app.bpartners.api.endpoint.rest.model.DetectionTracking;
import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.service.detection.DetectionTrackingService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DetectionTrackingController {
  private final DetectionTrackingService detectionTrackingService;
  private final DetectionTrackingRestMapper trackingRestMapper;

  @PostMapping("/users/{userId}/detectionTracking")
  public List<DetectionTracking> registerDetection(
      @PathVariable String userId, @RequestBody List<CreateDetectionTracking> restTracking) {
    var detectionTracking =
        restTracking.stream()
            .map(rest -> trackingRestMapper.toDomain(rest, AuthProvider.getAuthenticatedUser()))
            .toList();
    return detectionTrackingService
        .computeTrackingWithSubscriptionConsumptionLog(detectionTracking)
        .stream()
        .map(trackingRestMapper::toRest)
        .toList();
  }
}
