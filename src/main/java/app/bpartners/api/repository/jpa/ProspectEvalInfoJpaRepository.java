package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HProspectEvalInfo;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProspectEvalInfoJpaRepository extends JpaRepository<HProspectEvalInfo, String> {}
