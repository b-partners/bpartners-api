package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.Transaction;
import app.bpartners.api.endpoint.rest.model.TransactionCategory;
import java.math.BigDecimal;
import org.springframework.stereotype.Component;

@Component
public class TransactionRestMapper {

  public Transaction toRest(app.bpartners.api.model.Transaction internal) {
    Transaction transaction = new Transaction();
    transaction.setId(internal.getId());
    transaction.setAmount(BigDecimal.valueOf(internal.getAmount()));
    transaction.setLabel(internal.getLabel());
    transaction.setPaymentDatetime(internal.getPaymentDatetime());
    transaction.setReference(internal.getReference());
    transaction.setCategory(toRest(internal.getCategory()));
    return transaction;
  }

  public TransactionCategory toRest(app.bpartners.api.model.TransactionCategory internal) {
    TransactionCategory category = new TransactionCategory();
    category.setId(internal.getId());
    category.setLabel(internal.getLabel());
    return category;
  }
}
