package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.Transaction;
import app.bpartners.api.endpoint.rest.model.TransactionCategory;
import app.bpartners.api.endpoint.rest.validator.TransactionValidator;
import app.bpartners.api.service.TransactionCategoryTypeService;
import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class TransactionRestMapper {
  private final TransactionCategoryTypeService typeService;
  private final TransactionValidator transactionValidator;

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
    return new TransactionCategory()
        .id(internal.getType().getId())
        .label(internal.getType().getLabel())
        .comment(internal.getComment());
  }

  public app.bpartners.api.model.Transaction toDomain(Transaction rest) {
    transactionValidator.accept(rest);
    return app.bpartners.api.model.Transaction.builder()
        .id(rest.getId())
        .label(rest.getLabel())
        .reference(rest.getReference())
        .paymentDatetime(rest.getPaymentDatetime())
        .category(toDomain(rest.getId(), rest.getCategory()))
        .amount(null)
        .build();
  }

  public app.bpartners.api.model.TransactionCategory toDomain(
      String transactionId,
      TransactionCategory rest) {
    return app.bpartners.api.model.TransactionCategory.builder()
        .idTransaction(transactionId)
        .type(typeService.getCategoryById(rest.getId()))
        .comment(rest.getComment())
        .build();
  }
}
