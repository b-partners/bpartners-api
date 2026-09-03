package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.detection.HDetectionTracking;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface DetectionTrackingJpaRepository extends JpaRepository<HDetectionTracking, String> {
  List<HDetectionTracking> findAllByIdUserAndCreationDatetimeBetween(
      String idUser, Instant from, Instant to);

  Optional<HDetectionTracking> findByDetectionIdentifier(String detectionIdentifier);

  @Query(
      "SELECT t FROM HDetectionTracking t WHERE t.idUser = :idUser AND (CAST(:search AS string)"
          + " IS NULL"
          + " OR LOWER(t.zone) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))"
          + " OR LOWER(t.address) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))"
          + " OR LOWER(t.initiatorName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))"
          + " OR LOWER(t.initiatorEmail) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))"
          + " OR LOWER(t.initiatorPhoneNumber) LIKE LOWER(CONCAT('%', CAST(:search AS string),"
          + " '%')))"
          + " ORDER BY t.creationDatetime DESC")
  List<HDetectionTracking> findAllByIdUserAndSearch(
      @Param("idUser") String idUser, @Param("search") String search, Pageable pageable);
}
