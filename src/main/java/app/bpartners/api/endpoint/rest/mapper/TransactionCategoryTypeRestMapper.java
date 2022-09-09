package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.CreateTransactionCategoryType;
import app.bpartners.api.endpoint.rest.model.TransactionCategoryType;
import app.bpartners.api.endpoint.rest.validator.TransactionCategoryTypeValidator;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class TransactionCategoryTypeRestMapper {
  private final TransactionCategoryTypeValidator transactionCategoryTypeValidator;

  public TransactionCategoryType toRest(app.bpartners.api.model.TransactionCategoryType domain) {
    return new TransactionCategoryType()
        .id(domain.getId())
        .label(domain.getLabel());
  }

  public app.bpartners.api.model.TransactionCategoryType toDomain(
      CreateTransactionCategoryType rest) {
    transactionCategoryTypeValidator.accept(rest);
    return app.bpartners.api.model.TransactionCategoryType.builder()
        .label(rest.getLabel())
        .build();
  }
}
