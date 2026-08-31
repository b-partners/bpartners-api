package app.bpartners.api.repository;

import app.bpartners.api.model.detection.DetectionTracking;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface DetectionTrackingRepository {

  Optional<DetectionTracking> findByDetectionIdentifier(String detectionIdentifier);

  List<DetectionTracking> saveAll(List<DetectionTracking> trackings);

  List<DetectionTracking> findAllByIdUserBetween(String idUser, Instant from, Instant to);
}
