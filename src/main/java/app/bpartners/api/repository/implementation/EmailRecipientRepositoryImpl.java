package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.EmailRecipient;
import app.bpartners.api.model.mapper.EmailRecipientMapper;
import app.bpartners.api.repository.EmailRecipientRepository;
import app.bpartners.api.repository.jpa.EmailRecipientJpaRepository;
import app.bpartners.api.repository.jpa.model.HEmailRecipient;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@AllArgsConstructor
public class EmailRecipientRepositoryImpl implements EmailRecipientRepository {
  private final EmailRecipientJpaRepository jpaRepository;
  private final EmailRecipientMapper mapper;

  @Override
  public List<EmailRecipient> findByAccountHolderId(String accountHolderId) {
    return jpaRepository.findByIdAccountHolder(accountHolderId).stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Transactional
  @Override
  public List<EmailRecipient> saveAll(String accountHolderId, List<EmailRecipient> recipients) {
    jpaRepository.deleteByIdAccountHolder(accountHolderId);
    List<HEmailRecipient> entities = recipients.stream().map(mapper::toEntity).toList();
    return jpaRepository.saveAll(entities).stream().map(mapper::toDomain).toList();
  }
}
