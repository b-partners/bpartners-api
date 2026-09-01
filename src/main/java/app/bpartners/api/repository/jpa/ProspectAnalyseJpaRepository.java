package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HProspectAnalyse;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProspectAnalyseJpaRepository extends JpaRepository<HProspectAnalyse, String> {
  List<HProspectAnalyse> findAllByIdProspect(String idProspect);
}
