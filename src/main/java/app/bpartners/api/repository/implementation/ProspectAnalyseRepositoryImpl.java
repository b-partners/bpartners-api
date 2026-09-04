package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.mapper.ProspectAnalyseMapper;
import app.bpartners.api.model.prospect.ProspectAnalyse;
import app.bpartners.api.repository.ProspectAnalyseRepository;
import app.bpartners.api.repository.jpa.ProspectAnalyseJpaRepository;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class ProspectAnalyseRepositoryImpl implements ProspectAnalyseRepository {
  private final ProspectAnalyseJpaRepository jpaRepository;
  private final ProspectAnalyseMapper mapper;

  @Override
  public ProspectAnalyse save(ProspectAnalyse prospectAnalyse) {
    return mapper.toDomain(jpaRepository.save(mapper.toEntity(prospectAnalyse)));
  }

  @Override
  public List<ProspectAnalyse> findAllByIdProspect(String idProspect) {
    return jpaRepository.findAllByIdProspect(idProspect).stream().map(mapper::toDomain).toList();
  }

  @Override
  public Optional<ProspectAnalyse> findById(String id) {
    return jpaRepository.findById(id).map(mapper::toDomain);
  }
}
