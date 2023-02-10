package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.Attachment;
import app.bpartners.api.endpoint.rest.model.CreateAttachment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AttachmentRestMapper {
  public app.bpartners.api.model.Attachment toDomain(CreateAttachment createAttachment) {
    return app.bpartners.api.model.Attachment.builder()
        .name(createAttachment.getName())
        .content(createAttachment.getContent())
        .build();
  }

  public Attachment toRest(app.bpartners.api.model.Attachment attachment) {
    return new app.bpartners.api.endpoint.rest.model.Attachment()
        .name(attachment.getName())
        .fileId(attachment.getFileId());
  }
}
