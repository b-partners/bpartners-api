package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.PreUser;
import app.bpartners.api.model.mapper.PreUserMapper;
import app.bpartners.api.repository.PreUserRepository;
import app.bpartners.api.repository.jpa.PreUserJpaRepository;
import app.bpartners.api.repository.jpa.model.HPreUser;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class PreUserRepositoryImpl implements PreUserRepository {
  private final PreUserJpaRepository repository;
  private final PreUserMapper mapper;

  @Transactional
  @Override
  public List<PreUser> saveAll(List<PreUser> toCreate) {
    List<HPreUser> entityPreUsers = mapper.toEntity(toCreate);
    return repository.saveAll(entityPreUsers).stream().map(mapper::toDomain).toList();
  }
}
