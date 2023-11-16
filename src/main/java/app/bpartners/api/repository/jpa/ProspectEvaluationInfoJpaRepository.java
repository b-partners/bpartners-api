package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HProspectEvaluationInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProspectEvaluationInfoJpaRepository extends JpaRepository<HProspectEvaluationInfo, String> {
}
