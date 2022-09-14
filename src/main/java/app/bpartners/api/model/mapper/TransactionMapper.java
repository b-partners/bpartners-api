package app.bpartners.api.model.mapper;

import app.bpartners.api.model.Transaction;
import app.bpartners.api.model.TransactionCategory;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {
  public Transaction toDomain(app.bpartners.api.repository.swan.model.Transaction external,
                              TransactionCategory category) {
    return Transaction.builder()
        .id(external.getNode().getId())
        .amount(external.getNode().getAmount().getValue())
        .currency(external.getNode().getAmount().getCurrency())
        .label(external.getNode().getLabel())
        .reference(external.getNode().getReference())
        .paymentDatetime(external.getNode().getCreatedAt())
        .category(category)
        .build();
  }
}
