package app.bpartners.api.model.mapper;

import app.bpartners.api.model.TransactionCategory;
import app.bpartners.api.model.TransactionCategoryTemplate;
import app.bpartners.api.repository.jpa.TransactionCategoryJpaRepository;
import app.bpartners.api.repository.jpa.TransactionCategoryTemplateJpaRepository;
import app.bpartners.api.repository.jpa.model.HTransactionCategory;
import app.bpartners.api.repository.jpa.model.HTransactionCategoryTemplate;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class TransactionCategoryMapper {
  private final TransactionCategoryTemplateJpaRepository templateJpaRepository;
  private final TransactionCategoryJpaRepository jpaRepository;

  public TransactionCategory toDomain(HTransactionCategory entity, LocalDate from,
                                      LocalDate to) {
    long typeCount;
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
    typeCount = jpaRepository.countByCriteria(domain.getIdAccount(), domain.getType(),
        from.atStartOfDay(),
        to.plusDays(1).atStartOfDay().minusSeconds(1));
    if (typeCount == 0L) {
      typeCount = 1L;
    }
    domain.setTypeCount(typeCount);
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
