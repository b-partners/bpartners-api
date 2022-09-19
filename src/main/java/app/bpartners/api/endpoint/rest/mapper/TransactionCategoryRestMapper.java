package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.CreateTransactionCategory;
import app.bpartners.api.endpoint.rest.model.TransactionCategory;
import org.springframework.stereotype.Component;

@Component
public class TransactionCategoryRestMapper {

  public TransactionCategory toRest(app.bpartners.api.model.TransactionCategory domain) {
    return new TransactionCategory()
        .id(domain.getId())
        .vat(domain.getVat())
        .type(domain.getType())
        .userDefined(domain.isUserDefined());
  }

  public app.bpartners.api.model.TransactionCategory toDomain(
      String transactionId,
      String accountId,
      CreateTransactionCategory rest) {
    return app.bpartners.api.model.TransactionCategory.builder()
        .idTransaction(transactionId)
        .idAccount(accountId)
        .type(rest.getType())
        .vat(rest.getVat())
        .build();
  }
}
