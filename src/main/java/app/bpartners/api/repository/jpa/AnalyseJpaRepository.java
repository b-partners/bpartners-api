package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HAnalyse;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalyseJpaRepository extends JpaRepository<HAnalyse, String> {
  List<HAnalyse> findAllByIdProspect(String idProspect);
}
