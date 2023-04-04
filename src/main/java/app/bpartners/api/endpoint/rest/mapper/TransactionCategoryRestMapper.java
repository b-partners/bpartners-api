package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.CreateTransactionCategory;
import app.bpartners.api.endpoint.rest.model.TransactionCategory;
import app.bpartners.api.endpoint.rest.model.TransactionTypeEnum;
import app.bpartners.api.endpoint.rest.validator.CreateTransactionCategoryValidator;
import app.bpartners.api.model.TransactionCategoryTemplate;
import app.bpartners.api.model.exception.BadRequestException;
import app.bpartners.api.repository.TransactionCategoryTemplateRepository;
import app.bpartners.api.repository.TransactionRepository;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import static app.bpartners.api.service.utils.FractionUtils.parseFraction;

@Component
@AllArgsConstructor
public class TransactionCategoryRestMapper {
  private final TransactionCategoryTemplateRepository categoryTemplateRep;
  private final TransactionRepository transactionRepository;
  private final CreateTransactionCategoryValidator validator;

  public TransactionCategory toRest(app.bpartners.api.model.TransactionCategoryTemplate domain) {
    return new TransactionCategory()
        .id(domain.getId())
        .vat(domain.getVat().getCentsRoundUp())
        .type(domain.getType())
        .transactionType(domain.getTransactionType())
        .count(domain.getCount())
        .description(domain.getDescription())
        .isOther(domain.isOther());
  }

  public TransactionCategory toRest(app.bpartners.api.model.TransactionCategory domain) {
    return new TransactionCategory()
        .id(domain.getId())
        .vat(domain.getVat().getCentsRoundUp())
        .type(domain.getType())
        .transactionType(domain.getTransactionType())
        .count(domain.getTypeCount())
        .description(domain.getDescription())
        .isOther(domain.isOther())
        .comment(domain.getComment());
  }


  public app.bpartners.api.model.TransactionCategory toDomain(
      String transactionId, String accountId, CreateTransactionCategory rest) {
    validator.accept(rest);
    List<TransactionCategoryTemplate> categories = categoryTemplateRep.findByType(rest.getType());
    TransactionTypeEnum transactionType = transactionRepository.findById(transactionId).getType();
    TransactionCategoryTemplate categoryTemplate =
        categories.size() == 1
            ? categories.get(0)
            : categoryTemplateRep.findByTypeAndTransactionType(rest.getType(), transactionType);

    if (categoryTemplate.getTransactionType() != null
        && !transactionType.equals(categoryTemplate.getTransactionType())) {
      throw new BadRequestException(
          "Cannot add category." + categoryTemplate.getId() + " of type "
              + categoryTemplate.getTransactionType()
              + " to transaction." + transactionId + " of type "
              + transactionType
      );
    }

    return app.bpartners.api.model.TransactionCategory.builder()
        .idTransaction(transactionId)
        .idAccount(accountId)
        .type(rest.getType())
        .vat(parseFraction(rest.getVat()))
        .idTransactionCategoryTmpl(categoryTemplate.getId())
        .transactionType(categoryTemplate.getTransactionType())
        .other(categoryTemplate.isOther())
        .comment(categoryTemplate.isOther() ? rest.getComment() : null)
        .build();
  }
}
