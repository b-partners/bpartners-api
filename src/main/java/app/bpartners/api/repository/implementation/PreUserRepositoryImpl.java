package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.PreUser;
import app.bpartners.api.model.mapper.ContactMapper;
import app.bpartners.api.model.mapper.PreUserMapper;
import app.bpartners.api.repository.PreUserRepository;
import app.bpartners.api.repository.jpa.PreUserJpaRepository;
import app.bpartners.api.repository.jpa.model.HPreUser;
import app.bpartners.api.repository.sendinblue.ContactRepository;
import java.time.Instant;
import java.util.List;
import javax.transaction.Transactional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class PreUserRepositoryImpl implements PreUserRepository {
  private final PreUserJpaRepository repository;
  private final PreUserMapper mapper;
  private final ContactRepository contactRepository;
  private final ContactMapper contactMapper;

  @Transactional
  @Override
  public List<PreUser> saveAll(List<PreUser> toCreate) {
    contactRepository.save(
        toCreate.stream()
            .map(
                preUser -> {
                  Double contactId = // TODO: change this type double to appropriate type
                      (double) Instant.now().toEpochMilli();
                  return contactMapper.toSendinblueContact(contactId, preUser);
                })
            .toList());
    List<HPreUser> entityPreUsers = mapper.toEntity(toCreate);
    return repository.saveAll(entityPreUsers).stream().map(mapper::toDomain).toList();
  }
}
