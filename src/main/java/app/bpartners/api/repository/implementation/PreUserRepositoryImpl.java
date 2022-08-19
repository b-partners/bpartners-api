package app.bpartners.api.repository.implementation;

import app.bpartners.api.endpoint.rest.model.CreatePreUser;
import app.bpartners.api.model.PreUser;
import app.bpartners.api.model.entity.HPreUser;
import app.bpartners.api.model.mapper.PreUserMapper;
import app.bpartners.api.repository.PreUserRepository;
import app.bpartners.api.repository.jpa.PreUserJpaRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class PreUserRepositoryImpl implements PreUserRepository {
  private PreUserJpaRepository repository;
  private PreUserMapper mapper;

  @Override
  public PreUser getPreUserById(String id) {
    HPreUser hPreUser = repository.getById(id);
    return mapper.HtoDomain(hPreUser);
  }

  @Override
  public List<PreUser> getPreUsers(Pageable pageable) {
    return repository.findAll(pageable).stream().map(mapper::HtoDomain).toList();
  }

  @Override
  public List<PreUser> savePreUsers(List<HPreUser> hPreUsers) {
    return repository.saveAll(hPreUsers).stream().map(mapper::HtoDomain).toList();
  }
}
