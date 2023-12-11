package app.bpartners.api.endpoint.rest.validator;

import app.bpartners.api.endpoint.rest.model.CreateEmail;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.service.utils.EmailUtils;
import java.util.function.Consumer;
import org.springframework.stereotype.Component;

import static app.bpartners.api.service.utils.EmailUtils.allowedTags;

@Component
public class EmailRestValidator implements Consumer<CreateEmail> {
  @Override
  public void accept(CreateEmail email) {
    if (email.getAttachments() != null && !email.getAttachments().isEmpty()) {
      throw new NotImplementedException("Email does not support attachments for now");
    }
    StringBuilder builder = new StringBuilder();
    if (email.getId() == null) {
      builder.append("Attribute `id` is mandatory. ");
    }
    if (email.getEmailObject() == null) {
      builder.append("Attribute `emailObject` is mandatory. ");
    }
    if (email.getEmailBody() == null) {
      builder.append("Attribute `emailBody` is mandatory. ");
    } else {
      if (EmailUtils.hasMalformedTags(email.getEmailBody())) {
        builder.append(
                "Your HTML syntax is malformed or you use other tags " + "than these allowed : ")
            .append(allowedTags());
      }
    }
    if (email.getStatus() == null) {
      builder.append("Attribute `status` is mandatory. ");
    }
    if (email.getRecipients() == null) {
      builder.append("Attribute `recipients` is mandatory. ");
    } else if (email.getRecipients().isEmpty()) {
      builder.append("At least one recipient is mandatory. ");
    } else if (email.getRecipients().size() > 1) {
      builder.append("At most one recipient is suported for now. ");
    }
    String exceptionMessage = builder.toString();
    if (!exceptionMessage.isEmpty()) {
      throw new BadRequestException(exceptionMessage);
    }
  }
}
