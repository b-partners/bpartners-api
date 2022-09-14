package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.Transaction;
import app.bpartners.api.endpoint.rest.model.TransactionCategory;
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

  public TransactionCategory toRest(app.bpartners.api.model.TransactionCategory internal) {
    //TODO: map Rest attributes with domain
    return new TransactionCategory()
        .id(internal.getId())
        .type(internal.getType())
        .vat(internal.getVat())
        .userDefined(internal.isUserDefined());
  }

  public app.bpartners.api.model.Transaction toDomain(Transaction rest) {
    return app.bpartners.api.model.Transaction.builder()
        .id(rest.getId())
        .label(rest.getLabel())
        .reference(rest.getReference())
        .paymentDatetime(rest.getPaymentDatetime())
        .amount(null) //TODO : change this into int
        .build();
  }

  public app.bpartners.api.model.TransactionCategory toDomain(
      String transactionId,
      TransactionCategory rest) {
    //TODO: map Rest attributes with domain
    return app.bpartners.api.model.TransactionCategory.builder()
        .id(rest.getId())
        .userDefined(rest.getUserDefined())
        .vat(rest.getVat())
        .type(rest.getType())
        .idTransaction(transactionId)
        .build();
  }
}
