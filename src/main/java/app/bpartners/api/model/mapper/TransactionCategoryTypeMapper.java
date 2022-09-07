package app.bpartners.api.model.mapper;

import app.bpartners.api.model.TransactionCategoryType;
import app.bpartners.api.repository.jpa.model.HTransactionCategoryType;
import org.springframework.stereotype.Component;

@Component
public class TransactionCategoryTypeMapper {

  public TransactionCategoryType toDomain(HTransactionCategoryType entity) {
    return TransactionCategoryType.builder()
        .id(entity.getId())
        .label(entity.getLabel())
        .build();
  }

  public HTransactionCategoryType toEntity(TransactionCategoryType category) {
    return HTransactionCategoryType.builder()
        .id(category.getId())
        .label(category.getLabel())
        .build();
  }
}
