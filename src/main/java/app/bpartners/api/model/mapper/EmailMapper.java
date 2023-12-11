package app.bpartners.api.model.mapper;

import app.bpartners.api.model.Email;
import app.bpartners.api.repository.jpa.model.HEmail;
import app.bpartners.api.service.utils.DataTypeUtils;
import java.time.Instant;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class EmailMapper {
  private final AttachmentMapper attachmentMapper;

  public Email toDomain(HEmail entity) {
    return Email.builder()
        .id(entity.getId())
        .idUser(entity.getIdUser())
        .recipients(DataTypeUtils.decodeJsonList(entity.getRecipients()))
        .object(entity.getObject())
        .body(entity.getBody())
        .status(entity.getStatus())
        .attachments(entity.getAttachments().stream()
            .map(attachmentMapper::toDomain)
            .toList())
        .sendingDatetime(entity.getSendingDatetime())
        .updatedAt(entity.getUpdatedAt())
        .build();
  }

  public HEmail toEntity(Email domain) {
    return HEmail.builder()
        .id(domain.getId())
        .idUser(domain.getIdUser())
        .object(domain.getObject())
        .body(domain.getBody())
        .recipients(DataTypeUtils.encodeJsonList(domain.getRecipients()))
        .status(domain.getStatus())
        .attachments(domain.getAttachments().stream()
            .map(attachmentMapper::toEntity)
            .toList())
        .sendingDatetime(domain.getSendingDatetime())
        .updatedAt(Instant.now())
        .build();
  }
}
