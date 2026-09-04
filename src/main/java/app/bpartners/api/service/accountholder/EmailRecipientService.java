package app.bpartners.api.service.accountholder;

import app.bpartners.api.endpoint.rest.model.EmailRecipientType;
import app.bpartners.api.model.EmailRecipient;
import app.bpartners.api.repository.AccountHolderRepository;
import app.bpartners.api.repository.EmailRecipientRepository;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class EmailRecipientService {
  private final EmailRecipientRepository emailRecipientRepository;
  private final AccountHolderRepository accountHolderRepository;

  public List<EmailRecipient> getByAccountHolderId(String accountHolderId) {
    accountHolderRepository.findById(accountHolderId);
    return emailRecipientRepository.findByAccountHolderId(accountHolderId);
  }

  @Transactional
  public List<EmailRecipient> configure(String accountHolderId, List<EmailRecipient> recipients) {
    accountHolderRepository.findById(accountHolderId);
    Map<EmailRecipientType, List<EmailRecipient>> recipientsByType =
        recipients.stream().collect(Collectors.groupingBy(EmailRecipient::getType));
    recipientsByType.forEach(
        (type, typeRecipients) ->
            emailRecipientRepository.saveByType(
                accountHolderId, type, distinctByEmail(typeRecipients)));
    return emailRecipientRepository.findByAccountHolderId(accountHolderId);
  }

  private static List<EmailRecipient> distinctByEmail(List<EmailRecipient> recipients) {
    return new ArrayList<>(
        recipients.stream()
            .collect(
                Collectors.toMap(
                    EmailRecipient::getEmail,
                    Function.identity(),
                    (existing, ignored) -> existing,
                    LinkedHashMap::new))
            .values());
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
