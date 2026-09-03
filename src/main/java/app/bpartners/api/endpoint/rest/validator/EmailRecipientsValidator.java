package app.bpartners.api.endpoint.rest.validator;

import static app.bpartners.api.service.utils.EmailUtils.isValidEmail;

import app.bpartners.api.endpoint.rest.model.EmailRecipient;
import app.bpartners.api.endpoint.rest.model.EmailRecipientsConfiguration;
import app.bpartners.api.model.exception.BadRequestException;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

@Component
public class EmailRecipientsValidator implements Consumer<EmailRecipientsConfiguration> {
  @Override
  public void accept(EmailRecipientsConfiguration configuration) {
    StringBuilder messageBuilder = new StringBuilder();
    if (configuration.getRecipients() != null) {
      for (EmailRecipient recipient : configuration.getRecipients()) {
        if (recipient.getType() == null) {
          messageBuilder.append("recipient type is mandatory. ");
        }
        if (recipient.getEmails() != null) {
          recipient.getEmails().stream()
              .filter(email -> !isValidEmail(email))
              .forEach(email -> messageBuilder.append("Invalid email ").append(email).append(". "));
        }
      }
    }
    String message = messageBuilder.toString();
    if (!message.isEmpty()) {
      throw new BadRequestException(message);
    }
  }
}
