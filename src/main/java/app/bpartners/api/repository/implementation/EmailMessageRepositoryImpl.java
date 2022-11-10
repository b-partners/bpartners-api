package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.EmailMessage;
import app.bpartners.api.model.mapper.EmailMessageMapper;
import app.bpartners.api.repository.EmailMessageRepository;
import app.bpartners.api.repository.jpa.EmailMessageJpaRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class EmailMessageRepositoryImpl implements EmailMessageRepository {

  private final EmailMessageJpaRepository emailMessageJpaRepository;
  private final EmailMessageMapper emailMessageMapper;

  @Override
  public EmailMessage getByAccountId(String accountId) {
    return emailMessageMapper
        .toDomain(emailMessageJpaRepository.getByIdAccount(accountId));
  }

  @Override
  public EmailMessage getDefaultMessage() {
    return emailMessageMapper
        .toDomain(emailMessageJpaRepository.findAll().get(0));
  }
}
