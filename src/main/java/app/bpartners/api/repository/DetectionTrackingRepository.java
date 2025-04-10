package app.bpartners.api.repository;

import app.bpartners.api.model.detection.DetectionTracking;
import java.time.Instant;
import java.util.List;

public interface DetectionTrackingRepository {
  List<DetectionTracking> saveAll(List<DetectionTracking> trackings);

  List<DetectionTracking> findAllByIdUserBetween(String idUser, Instant from, Instant to);
}
