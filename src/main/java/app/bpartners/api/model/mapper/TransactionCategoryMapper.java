package app.bpartners.api.model.mapper;

import app.bpartners.api.model.TransactionCategory;
import app.bpartners.api.repository.jpa.model.HTransactionCategory;
import org.springframework.stereotype.Component;

@Component
public class TransactionCategoryMapper {

  public TransactionCategory toDomain(HTransactionCategory entity) {
    return TransactionCategory.builder()
        .id(entity.getId())
        .label(entity.getLabel())
        .build();
  }

  public HTransactionCategory toEntity(TransactionCategory category) {
    return HTransactionCategory.builder()
        .id(category.getId())
        .label(category.getLabel())
        .build();
  }
}
