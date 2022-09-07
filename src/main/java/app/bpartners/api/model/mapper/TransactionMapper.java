package app.bpartners.api.model.mapper;

import app.bpartners.api.model.Transaction;
import app.bpartners.api.model.TransactionCategory;
import app.bpartners.api.model.TransactionCategoryType;
import app.bpartners.api.repository.jpa.model.HTransactionCategoryType;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {
  public Transaction toDomain(app.bpartners.api.repository.swan.model.Transaction external,
                              TransactionCategory category) {
    return Transaction.builder()
        .id(external.node.id)
        .amount(external.node.amount.value)
        .currency(external.node.amount.currency)
        .label(external.node.label)
        .reference(external.node.reference)
        .paymentDatetime(external.node.createdAt)
        .category(category)
        .build();
  }

  public TransactionCategoryType toDomain(HTransactionCategoryType entity) {
    return TransactionCategoryType.builder()
        .id(entity.getId())
        .label(entity.getLabel())
        .build();
  }
}
