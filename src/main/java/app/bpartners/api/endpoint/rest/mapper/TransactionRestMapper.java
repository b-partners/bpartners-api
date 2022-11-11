package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.Transaction;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class TransactionRestMapper {
  private final TransactionCategoryRestMapper categoryRestMapper;

  public Transaction toRest(app.bpartners.api.model.Transaction internal) {
    Transaction transaction = new Transaction()
        .id(internal.getSwanId())
        .amount(internal.getAmount().getApproximatedValue())
        .label(internal.getLabel())
        .paymentDatetime(internal.getPaymentDatetime())
        .reference(internal.getReference())
        .type(internal.getType());
    if (internal.getCategory() != null) {
      transaction.setCategory(List.of(categoryRestMapper.toRest(internal.getCategory())));
    }
    return transaction;
  }
}
