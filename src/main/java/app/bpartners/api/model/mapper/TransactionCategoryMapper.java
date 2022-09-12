package app.bpartners.api.model.mapper;

import app.bpartners.api.model.TransactionCategory;
import app.bpartners.api.repository.jpa.model.HTransactionCategory;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class TransactionCategoryMapper {

  public TransactionCategory toDomain(HTransactionCategory entity) {
    return TransactionCategory.builder()
        .id(entity.getId())
        .type(entity.getType())
        .vat(entity.getVat())
        .userDefined(entity.isUserDefined())
        .build();
  }

  public HTransactionCategory toEntity(TransactionCategory category) {
    return HTransactionCategory.builder()
        .id(category.getId())
        .type(category.getType())
        .idTransaction(category.getIdTransaction())
        .userDefined(category.isUserDefined())
        .vat(category.getVat())
        .build();
  }
}
