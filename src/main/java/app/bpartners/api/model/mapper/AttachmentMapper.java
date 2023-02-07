package app.bpartners.api.model.mapper;

import app.bpartners.api.model.Attachment;
import app.bpartners.api.repository.jpa.model.HAttachment;
import org.springframework.stereotype.Component;

@Component
public class AttachmentMapper {
  public Attachment toDomain(HAttachment entity) {
    return Attachment.builder()
        .fileId(entity.getIdFile())
        .name(entity.getName())
        .build();
  }

  public HAttachment toEntity(Attachment domain, String idInvoiceRelaunch) {
    return HAttachment.builder()
        .name(domain.getName())
        .idInvoiceRelaunch(idInvoiceRelaunch)
        .idFile(domain.getFileId())
        .build();
  }
}
