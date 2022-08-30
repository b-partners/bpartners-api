package app.bpartners.api.model.mapper;

import app.bpartners.api.model.Transaction;
import app.bpartners.api.model.TransactionCategory;
import app.bpartners.api.repository.jpa.model.HTransactionCategory;
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

  public TransactionCategory toDomain(HTransactionCategory entity) {
    return TransactionCategory.builder()
        .id(entity.getId())
        .label(entity.getLabel())
        .build();
  }
}
