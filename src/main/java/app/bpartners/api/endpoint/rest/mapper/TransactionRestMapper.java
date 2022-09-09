package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.Transaction;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class TransactionRestMapper {

  public Transaction toRest(app.bpartners.api.model.Transaction internal) {
    Transaction transaction = new Transaction();
    transaction.setId(internal.getId());
    transaction.setAmount(BigDecimal.valueOf(internal.getAmount()));
    transaction.setLabel(internal.getLabel());
    transaction.setPaymentDatetime(internal.getPaymentDatetime());
    transaction.setReference(internal.getReference());
    return transaction;
  }
}
