package app.bpartners.api.service;

import app.bpartners.api.model.EmailMessage;
import app.bpartners.api.repository.EmailMessageRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class EmailMessageService {
  private final EmailMessageRepository emailMessageRepository;

  public EmailMessage getEmailMessage(String accountId) {
    return emailMessageRepository.getByAccountId(accountId);
  }

  public EmailMessage getDefaultMessage() {
    return emailMessageRepository.getDefaultMessage();
  }
}
