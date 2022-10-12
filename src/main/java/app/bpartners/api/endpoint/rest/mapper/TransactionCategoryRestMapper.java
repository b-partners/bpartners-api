package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.CreateTransactionCategory;
import app.bpartners.api.endpoint.rest.model.TransactionCategory;
import app.bpartners.api.endpoint.rest.validator.CreateTransactionCategoryValidator;
import app.bpartners.api.model.TransactionCategoryTemplate;
import app.bpartners.api.repository.TransactionCategoryTemplateRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class TransactionCategoryRestMapper {
  private final TransactionCategoryTemplateRepository categoryTmplRepository;
  private final CreateTransactionCategoryValidator validator;

  public TransactionCategory toRest(app.bpartners.api.model.TransactionCategory domain) {
    return new TransactionCategory()
        .id(domain.getId())
        .vat(domain.getVat())
        .type(domain.getType())
        .userDefined(domain.isUserDefined())
        .count(domain.getTypeCount());
  }

  public app.bpartners.api.model.TransactionCategory toDomain(
      String transactionId,
      String accountId,
      CreateTransactionCategory rest) {
    validator.accept(rest);
    TransactionCategoryTemplate categoryTemplate = categoryTmplRepository.findByTypeAndVat(
        rest.getType(), rest.getVat());
    app.bpartners.api.model.TransactionCategory category =
        app.bpartners.api.model.TransactionCategory.builder()
            .idTransaction(transactionId)
            .idAccount(accountId)
            .type(rest.getType())
            .vat(rest.getVat())
            .build();
    if (categoryTemplate != null) {
      category.setIdTransactionCategoryTmpl(categoryTemplate.getId());
    }
    return category;
  }
}
