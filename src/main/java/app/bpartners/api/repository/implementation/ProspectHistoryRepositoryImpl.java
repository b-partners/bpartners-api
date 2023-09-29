package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.ProspectHistory;
import app.bpartners.api.repository.ProspectHistoryRepository;
import app.bpartners.api.repository.jpa.ProspectHistoryJpaRepository;
import app.bpartners.api.repository.jpa.model.HProspectHistory;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class ProspectHistoryRepositoryImpl implements ProspectHistoryRepository {
  private final ProspectHistoryJpaRepository jpaRepository;
  @Override
  public List<ProspectHistory> getAllByIdProspect(String idProspect) {
    List<HProspectHistory> entities = jpaRepository.findAllByIdProspect(idProspect);

    return null;
  }
}
