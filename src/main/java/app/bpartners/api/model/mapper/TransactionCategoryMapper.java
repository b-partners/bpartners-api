package app.bpartners.api.model.mapper;

import app.bpartners.api.model.TransactionCategory;
import app.bpartners.api.repository.jpa.model.HTransactionCategory;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class TransactionCategoryMapper {
  private final TransactionCategoryTypeMapper typeMapper;

  public TransactionCategory toDomain(HTransactionCategory entity) {
    return TransactionCategory.builder()
        .id(entity.getId())
        .type(typeMapper.toDomain(entity.getType()))
        .comment(entity.getComment())
        .build();
  }

  public HTransactionCategory toEntity(TransactionCategory category) {
    return HTransactionCategory.builder()
        .id(category.getId())
        .type(typeMapper.toEntity(category.getType()))
        .comment(category.getComment())
        .build();
  }
}
