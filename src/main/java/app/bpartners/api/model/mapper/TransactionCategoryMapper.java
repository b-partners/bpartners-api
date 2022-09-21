package app.bpartners.api.model.mapper;

import app.bpartners.api.model.TransactionCategory;
import app.bpartners.api.model.TransactionCategoryTemplate;
import app.bpartners.api.repository.jpa.TransactionCategoryTemplateJpaRepository;
import app.bpartners.api.repository.jpa.model.HTransactionCategory;
import app.bpartners.api.repository.jpa.model.HTransactionCategoryTemplate;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class TransactionCategoryMapper {
  private final TransactionCategoryTemplateJpaRepository templateJpaRepository;

  public TransactionCategory toDomain(HTransactionCategory entity) {
    TransactionCategory domain = TransactionCategory.builder()
        .id(entity.getId())
        .idAccount(entity.getIdAccount())
        .type(entity.getType())
        .vat(entity.getVat())
        .idTransactionCategoryTmpl(entity.getIdCategoryTemplate())
        .build();
    if (!entity.isUserDefined()) {
      HTransactionCategoryTemplate categoryTemplate =
          templateJpaRepository.getById(entity.getIdCategoryTemplate());
      domain.setType(categoryTemplate.getType());
      domain.setVat(categoryTemplate.getVat());
    }
    return domain;
  }

  public TransactionCategoryTemplate toDomain(HTransactionCategoryTemplate entity) {
    if (entity == null) {
      return null;
    }
    return TransactionCategoryTemplate.builder()
        .id(entity.getId())
        .type(entity.getType())
        .vat(entity.getVat())
        .build();
  }

  public HTransactionCategory toEntity(TransactionCategory category) {
    return HTransactionCategory.builder()
        .id(category.getId())
        .idAccount(category.getIdAccount())
        .type(category.getType())
        .idTransaction(category.getIdTransaction())
        .idCategoryTemplate(category.getIdTransactionCategoryTmpl())
        .vat(category.getVat())
        .build();
  }
}
