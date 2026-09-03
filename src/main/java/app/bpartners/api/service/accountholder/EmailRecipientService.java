package app.bpartners.api.service.accountholder;

import app.bpartners.api.endpoint.rest.model.EmailRecipientType;
import app.bpartners.api.model.EmailRecipient;
import app.bpartners.api.repository.AccountHolderRepository;
import app.bpartners.api.repository.EmailRecipientRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class EmailRecipientService {
  private final EmailRecipientRepository emailRecipientRepository;
  private final AccountHolderRepository accountHolderRepository;

  public List<EmailRecipient> getByAccountHolderId(String accountHolderId) {
    accountHolderRepository.findById(accountHolderId);
    return emailRecipientRepository.findByAccountHolderId(accountHolderId);
  }

  public List<EmailRecipient> configure(String accountHolderId, List<EmailRecipient> recipients) {
    accountHolderRepository.findById(accountHolderId);
    return emailRecipientRepository.saveAll(accountHolderId, recipients);
  }

  public List<String> getEmails(String accountHolderId, EmailRecipientType type) {
    return emailRecipientRepository.findByAccountHolderIdAndType(accountHolderId, type).stream()
        .map(EmailRecipient::getEmail)
        .toList();
  }

  public List<EmailRecipient> populateByType(
      String accountHolderId, EmailRecipientType type, List<String> emails) {
    List<EmailRecipient> recipients =
        emails.stream()
            .map(
                email ->
                    EmailRecipient.builder()
                        .idAccountHolder(accountHolderId)
                        .type(type)
                        .email(email)
                        .build())
            .toList();
    return emailRecipientRepository.saveByType(accountHolderId, type, recipients);
  }
}
