package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.Email;
import app.bpartners.api.model.mapper.EmailMapper;
import app.bpartners.api.repository.EmailRepository;
import app.bpartners.api.repository.jpa.EmailJpaRepository;
import app.bpartners.api.repository.jpa.model.HEmail;
import java.util.List;
import java.util.Optional;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class EmailRepositoryImpl implements EmailRepository {
  private final EmailJpaRepository jpaRepository;
  private final EmailMapper emailMapper;

  @Override
  public Email findById(String id) {
    Optional<HEmail> optionEmail = jpaRepository.findById(id);
    return optionEmail.map(emailMapper::toDomain)
        .orElse(null);
  }

  @Override
  public List<Email> findAllByUserId(String userId) {
    return jpaRepository.findAllByIdUser(userId).stream()
        .map(emailMapper::toDomain)
        .toList();
  }

  @Override
  public List<Email> saveAll(List<Email> emails) {
    List<HEmail> emailEntities = emails.stream()
        .map(emailMapper::toEntity)
        .toList();
    return jpaRepository.saveAll(emailEntities).stream()
        .map(emailMapper::toDomain)
        .toList();
  }

}
