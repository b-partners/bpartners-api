package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.detection.HDetectionTracking;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DetectionTrackingJpaRepository extends JpaRepository<HDetectionTracking, String> {
  List<HDetectionTracking> findAllByIdUserAndCreationDatetimeBetween(
      String idUser, Instant from, Instant to);
}
