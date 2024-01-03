package app.bpartners.api.model.mapper;

import app.bpartners.api.model.TransactionSupportingDocs;
import app.bpartners.api.repository.jpa.model.HTransactionSupportingDocs;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class TransactionSupportingDocsMapper {
  private final FileMapper fileMapper;

  public TransactionSupportingDocs toDomain(HTransactionSupportingDocs docEntity) {
    return TransactionSupportingDocs.builder()
        .id(docEntity.getId())
        .fileInfo(fileMapper.toDomain(docEntity.getFileInfo()))
        .build();
  }
}
