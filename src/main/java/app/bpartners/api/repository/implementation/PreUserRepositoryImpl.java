package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.PreUser;
import app.bpartners.api.repository.jpa.model.HPreUser;
import app.bpartners.api.model.mapper.PreUserMapper;
import app.bpartners.api.repository.PreUserRepository;
import app.bpartners.api.repository.jpa.PreUserJpaRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class PreUserRepositoryImpl implements PreUserRepository {
  private PreUserJpaRepository repository;
  private PreUserMapper mapper;

  @Override
  public List<PreUser> saveAll(List<PreUser> toCreate) {
    List<HPreUser> entityPreUsers = mapper.toEntity(toCreate);
    return repository.saveAll(entityPreUsers).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
  }

}
