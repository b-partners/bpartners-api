package app.bpartners.api.model.mapper;

import app.bpartners.api.model.Transaction;
import app.bpartners.api.model.TransactionCategory;
import app.bpartners.api.repository.jpa.model.HTransaction;
import org.springframework.stereotype.Component;

import static app.bpartners.api.service.utils.FractionUtils.parseFraction;

@Component
public class TransactionMapper {
  public Transaction toDomain(app.bpartners.api.repository.swan.model.Transaction external,
                              TransactionCategory category,
                              HTransaction entity) {
    return Transaction.builder()
        .id(entity.getId())
        .swanId(entity.getSwanId())
        .amount(parseFraction(external.getNode().getAmount().getValue()))
        .currency(external.getNode().getAmount().getCurrency())
        .label(external.getNode().getLabel())
        .reference(external.getNode().getReference())
        .paymentDatetime(external.getNode().getCreatedAt())
        .category(category)
        .type(entity.getType())
        .build();
  }
}
