package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.PreUser;
import app.bpartners.api.model.entity.HPreUser;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.model.mapper.PreUserMapper;
import app.bpartners.api.repository.PreUserRepository;
import app.bpartners.api.repository.jpa.PreUserJpaRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class PreUserRepositoryImpl implements PreUserRepository {
  private PreUserJpaRepository repository;
  private PreUserMapper mapper;

  @Override
  public List<PreUser> getPreUsers(Pageable pageable) {
    return repository.findAll(pageable).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public List<PreUser> createPreUsers(List<PreUser> toCreate) {
    List<HPreUser> hPreUsers = mapper.toEntity(toCreate);
    return repository.saveAll(hPreUsers).stream()
        .map(mapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public List<PreUser> getByCriteria(Pageable pageable, String firstName, String lastName,
                                     String email, String society, String phoneNumber) {
    throw new NotImplementedException("Not implemented yet");
  }
}
