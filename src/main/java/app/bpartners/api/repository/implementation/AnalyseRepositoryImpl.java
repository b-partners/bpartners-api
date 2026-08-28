package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.mapper.AnalyseMapper;
import app.bpartners.api.model.prospect.Analyse;
import app.bpartners.api.repository.AnalyseRepository;
import app.bpartners.api.repository.jpa.AnalyseJpaRepository;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class AnalyseRepositoryImpl implements AnalyseRepository {
  private final AnalyseJpaRepository jpaRepository;
  private final AnalyseMapper mapper;

  @Override
  public Analyse save(Analyse analyse) {
    return mapper.toDomain(jpaRepository.save(mapper.toEntity(analyse)));
  }

  @Override
  public List<Analyse> findAllByIdProspect(String idProspect) {
    return jpaRepository.findAllByIdProspect(idProspect).stream().map(mapper::toDomain).toList();
  }

  @Override
  public Optional<Analyse> findById(String id) {
    return jpaRepository.findById(id).map(mapper::toDomain);
  }

  @Override
  public void deleteById(String id) {
    jpaRepository.deleteById(id);
  }
}
