package app.bpartners.api.repository.jpa;

import app.bpartners.api.model.project.AnalysisProject;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AnalysisProjectRepository extends JpaRepository<AnalysisProject, String> {
  Optional<AnalysisProject> findByUserIdAndProjectKey(String userId, String projectKey);

  Optional<AnalysisProject> findByUserIdAndExternalReference(
      String userId, String externalReference);

  @Query(
      "SELECT p FROM analysis_project p WHERE p.userId = :userId"
          + " AND (CAST(:projectKey AS string) IS NULL OR p.projectKey = :projectKey)"
          + " AND (CAST(:externalReference AS string) IS NULL"
          + " OR p.externalReference = :externalReference)"
          + " AND (CAST(:search AS string) IS NULL"
          + " OR LOWER(p.zone) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))"
          + " OR LOWER(p.address) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))"
          + " OR LOWER(p.initiatorName) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))"
          + " OR LOWER(p.initiatorEmail) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))"
          + " OR LOWER(p.initiatorPhoneNumber)"
          + " LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))"
          + " OR LOWER(p.externalReference) LIKE LOWER(CONCAT('%', CAST(:search AS string),"
          + " '%')))"
          + " ORDER BY p.creationDatetime DESC")
  List<AnalysisProject> findAllByUserIdAndFilters(
      @Param("userId") String userId,
      @Param("projectKey") String projectKey,
      @Param("externalReference") String externalReference,
      @Param("search") String search,
      Pageable pageable);
}
