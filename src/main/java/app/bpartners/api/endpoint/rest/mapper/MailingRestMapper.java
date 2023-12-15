package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.CreateAttachment;
import app.bpartners.api.endpoint.rest.model.CreateEmail;
import app.bpartners.api.endpoint.rest.model.Email;
import app.bpartners.api.endpoint.rest.validator.EmailRestValidator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@AllArgsConstructor
public class MailingRestMapper {
  private final AttachmentRestMapper attachmentMapper;
  private final EmailRestValidator emailCreatorValidator;

  public Email toRest(app.bpartners.api.model.Email domain) {
    return new Email()
        .id(domain.getId())
        .recipients(domain.getRecipients())
        .emailObject(domain.getObject())
        .emailBody(domain.getBody())
        .status(domain.getStatus())
        .attachments(
            domain.getAttachments().stream()
                .map(attachmentMapper::toRest)
                .collect(Collectors.toList()))
        .sendingDatetime(domain.getSendingDatetime())
        .updatedAt(domain.getUpdatedAt());
  }

  public app.bpartners.api.model.Email toDomain(String idUser, CreateEmail rest) {
    emailCreatorValidator.accept(rest);
    List<CreateAttachment> attachments = rest.getAttachments();
    return app.bpartners.api.model.Email.builder()
        .id(rest.getId())
        .idUser(idUser)
        .recipients(rest.getRecipients())
        .object(rest.getEmailObject())
        .body(rest.getEmailBody())
        .status(rest.getStatus())
        .attachments(
            attachments == null
                ? List.of()
                : attachments.stream().map(attachmentMapper::toDomain).collect(Collectors.toList()))
        .build();
  }
}
