package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.ProspectHistory;
import app.bpartners.api.model.mapper.ProspectHistoryMapper;
import app.bpartners.api.repository.ProspectHistoryRepository;
import app.bpartners.api.repository.jpa.ProspectHistoryJpaRepository;
import app.bpartners.api.repository.jpa.model.HProspectHistory;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class ProspectHistoryRepositoryImpl implements ProspectHistoryRepository {
  private final ProspectHistoryJpaRepository jpaRepository;
  private final ProspectHistoryMapper mapper;
  @Override
  public List<ProspectHistory> getAllByIdProspect(String idProspect) {
    List<HProspectHistory> entities = jpaRepository.findAllByIdProspect(idProspect);
    return entities.stream()
        .map(mapper::toDomain)
        .collect(Collectors.toList());
  }

  @Override
  public ProspectHistory getLatestUpdateByIdProspect(String idProspect) {
    return mapper.toDomain(jpaRepository.findTopByIdProspectOrderByUpdatedAt(idProspect));
  }
}
