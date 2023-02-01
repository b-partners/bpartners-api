package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.Attachment;
import app.bpartners.api.model.mapper.AttachmentMapper;
import app.bpartners.api.repository.AttachmentRepository;
import app.bpartners.api.repository.jpa.AttachmentJpaRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@AllArgsConstructor
@Repository

public class AttachmentRepositoryImpl implements AttachmentRepository {
  private final AttachmentJpaRepository jpaRepository;
  private final AttachmentMapper mapper;

  @Override
  public List<Attachment> findByIdInvoiceRelaunch(String idInvoiceRelaunch) {
    return jpaRepository.findAllByIdInvoiceRelaunch(idInvoiceRelaunch)
        .stream()
        .map(mapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
  }
}