package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.CreateTransactionCategory;
import app.bpartners.api.endpoint.rest.model.TransactionCategory;
import org.springframework.stereotype.Component;

@Component
public class TransactionCategoryRestMapper {

  public TransactionCategory toRest(app.bpartners.api.model.TransactionCategory domain) {
    return new TransactionCategory()
        .id(domain.getId())
        .label(domain.getLabel());
  }

  public app.bpartners.api.model.TransactionCategory toDomain(CreateTransactionCategory rest) {
    return app.bpartners.api.model.TransactionCategory.builder()
        .label(rest.getLabel())
        .build();
  }
}
