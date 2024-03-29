package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.Attachment;
import app.bpartners.api.model.mapper.AttachmentMapper;
import app.bpartners.api.repository.AttachmentRepository;
import app.bpartners.api.repository.jpa.AttachmentJpaRepository;
import app.bpartners.api.repository.jpa.model.HAttachment;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@AllArgsConstructor
@Repository
public class AttachmentRepositoryImpl implements AttachmentRepository {
  private final AttachmentJpaRepository jpaRepository;
  private final AttachmentMapper mapper;

  @Override
  public List<Attachment> findByIdInvoiceRelaunch(String idInvoiceRelaunch) {
    return jpaRepository.findAllByIdInvoiceRelaunch(idInvoiceRelaunch).stream()
        .map(mapper::toDomain)
        .toList();
  }

  @Override
  public List<Attachment> saveAll(List<Attachment> attachments, String idInvoiceRelaunch) {
    List<HAttachment> entities =
        attachments.stream()
            .map(attachment -> mapper.toEntity(attachment, idInvoiceRelaunch))
            .toList();
    return jpaRepository.saveAll(entities).stream().map(mapper::toDomain).toList();
  }
}
