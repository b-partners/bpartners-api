package app.bpartners.api.repository.jpa;

import app.bpartners.api.repository.jpa.model.HProspectHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProspectHistoryJpaRepository extends JpaRepository<HProspectHistory, String> {
  List<HProspectHistory> findAllByIdProspect(String idProspect);
  HProspectHistory findTopByIdProspectOrderByUpdatedAt(String idProspect);
}
