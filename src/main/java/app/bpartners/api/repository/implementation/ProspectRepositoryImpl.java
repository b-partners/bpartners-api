package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.Prospect;
import app.bpartners.api.model.mapper.ProspectMapper;
import app.bpartners.api.repository.ProspectRepository;
import app.bpartners.api.repository.jpa.ProspectJpaRepository;
import app.bpartners.api.repository.jpa.model.HProspect;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@AllArgsConstructor
@Repository

public class ProspectRepositoryImpl implements ProspectRepository {
  private final ProspectJpaRepository jpaRepository;
  private final ProspectMapper mapper;

  @Override
  public List<Prospect> findAllByIdAccountHolder(String idAccountHolder) {
    return jpaRepository.findAllByIdAccountHolder(idAccountHolder)
        .stream()
        .map(mapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
  }

  @Transactional
  @Override
  public List<Prospect> saveAll(List<Prospect> prospects) {
    List<HProspect> entities = prospects
        .stream()
        .map(mapper::toEntity)
        .collect(Collectors.toUnmodifiableList());
    return jpaRepository.saveAll(entities)
        .stream()
        .map(mapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
  }
}
