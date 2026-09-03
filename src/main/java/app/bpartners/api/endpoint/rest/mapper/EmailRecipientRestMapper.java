package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.EmailRecipientsConfiguration;
import app.bpartners.api.endpoint.rest.model.EmailRecipientType;
import app.bpartners.api.endpoint.rest.validator.EmailRecipientsValidator;
import app.bpartners.api.model.EmailRecipient;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class EmailRecipientRestMapper {
  private final EmailRecipientsValidator validator;

  public List<EmailRecipient> toDomain(
      String accountHolderId, EmailRecipientsConfiguration rest) {
    validator.accept(rest);
    if (rest.getRecipients() == null) {
      return List.of();
    }
    return rest.getRecipients().stream()
        .flatMap(
            recipient ->
                recipient.getEmails().stream()
                    .map(
                        email ->
                            EmailRecipient.builder()
                                .idAccountHolder(accountHolderId)
                                .type(recipient.getType())
                                .email(email)
                                .build()))
        .toList();
  }

  public EmailRecipientsConfiguration toRest(List<EmailRecipient> domain) {
    Map<EmailRecipientType, List<String>> emailsByType =
        domain.stream()
            .collect(
                Collectors.groupingBy(
                    EmailRecipient::getType,
                    Collectors.mapping(EmailRecipient::getEmail, Collectors.toList())));
    List<app.bpartners.api.endpoint.rest.model.EmailRecipient> recipients =
        emailsByType.entrySet().stream()
            .map(
                entry ->
                    new app.bpartners.api.endpoint.rest.model.EmailRecipient()
                        .type(entry.getKey())
                        .emails(entry.getValue()))
            .toList();
    return new EmailRecipientsConfiguration().recipients(recipients);
  }
}
