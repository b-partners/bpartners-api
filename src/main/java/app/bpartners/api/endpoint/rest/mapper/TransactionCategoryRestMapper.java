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

import static app.bpartners.api.service.utils.FractionUtils.parseFraction;

@Component
@AllArgsConstructor
public class TransactionCategoryRestMapper {
  private final TransactionCategoryTemplateRepository categoryTmplRepository;
  private final TransactionRepository transactionRepository;
  private final CreateTransactionCategoryValidator validator;

  public TransactionCategory toRest(app.bpartners.api.model.TransactionCategory domain) {
    return new TransactionCategory()
        .id(domain.getId())
        .vat(domain.getVat().getCentsRoundUp())
        .type(domain.getType())
        .transactionType(domain.getTransactionType())
        .count(domain.getTypeCount())
        .description(domain.getDescription())
        .comment(domain.getComment());
  }

  public app.bpartners.api.model.TransactionCategory toDomain(
      String transactionId,
      String accountId,
      CreateTransactionCategory rest) {
    validator.accept(rest);
    TransactionCategoryTemplate categoryTemplate =
        categoryTmplRepository.findByType(rest.getType());
    Transaction transaction = transactionRepository.findByAccountIdAndId(accountId, transactionId);
    if (!transaction.getType().equals(categoryTemplate.getTransactionType())) {
      throw new BadRequestException(
          "Cannot add category." + categoryTemplate.getId() + " of type "
              + categoryTemplate.getTransactionType()
              + " to transaction." + transactionId + " of type "
              + transaction.getType()
      );
    }
    app.bpartners.api.model.TransactionCategory domain =
        app.bpartners.api.model.TransactionCategory.builder()
            .idTransaction(transactionId)
            .idAccount(accountId)
            .type(rest.getType())
            .vat(parseFraction(rest.getVat()))
            .idTransactionCategoryTmpl(categoryTemplate.getId())
            .transactionType(categoryTemplate.getTransactionType())
            .build();
    if (categoryTemplate.isOther()) {
      domain.setComment(rest.getComment());
    }
    return domain;
  }
}
