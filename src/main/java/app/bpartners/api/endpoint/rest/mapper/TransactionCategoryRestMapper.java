package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.CreateTransactionCategory;
import app.bpartners.api.endpoint.rest.model.TransactionCategory;
import app.bpartners.api.endpoint.rest.validator.CreateTransactionCategoryValidator;
import app.bpartners.api.model.Transaction;
import app.bpartners.api.model.TransactionCategoryTemplate;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.repository.TransactionCategoryTemplateRepository;
import app.bpartners.api.repository.TransactionRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class TransactionCategoryRestMapper {
  private final TransactionCategoryTemplateRepository categoryTmplRepository;
  private final CreateTransactionCategoryValidator validator;
  private final TransactionRepository transactionRepository;

  public TransactionCategory toRest(app.bpartners.api.model.TransactionCategory domain) {
    return new TransactionCategory()
        .id(domain.getId())
        .type(domain.getType())
        .comment(domain.getComment())
        .count(domain.getTypeCount())
        .description(domain.getDescription())
        .transactionType(domain.getTransactionType());
  }

  public app.bpartners.api.model.TransactionCategory toDomain(
      String transactionId,
      String accountId,
      CreateTransactionCategory rest) {
    validator.accept(rest);
    Transaction persisted = transactionRepository.findById(transactionId);
    String categoryType = rest.getType();
    TransactionCategoryTemplate categoryTemplate = categoryTmplRepository.findByType(categoryType);
    app.bpartners.api.model.TransactionCategory category =
        app.bpartners.api.model.TransactionCategory.builder()
            .idTransaction(transactionId)
            .idTransactionCategoryTmpl(categoryTemplate.getId())
            .idAccount(accountId)
            .type(categoryType)
            .description(rest.getDescription())
            .build();
    if (categoryTemplate.isOther()) {
      category.setComment(rest.getComment());
    } else {
      if (rest.getComment() != null) {
        throw new BadRequestException(
            "Transaction category of type " + categoryTemplate.getType() + " cannot have comment");
      }
    }
    if (persisted.getType() != categoryTemplate.getTransactionType()) {
      throw new BadRequestException(
          "Transaction category of type " + categoryTemplate.getTransactionType()
              + " cannot be added to transaction of type " + persisted.getType());
    }
    return category;
  }
}