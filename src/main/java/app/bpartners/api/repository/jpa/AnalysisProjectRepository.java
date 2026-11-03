package app.bpartners.api.repository.jpa;

import app.bpartners.api.model.project.AnalysisProject;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnalysisProjectRepository extends JpaRepository<AnalysisProject, String> {
  Optional<AnalysisProject> findByUserIdAndProjectKey(String userId, String projectKey);

  Optional<AnalysisProject> findByUserIdAndExternalReference(
      String userId, String externalReference);
}
