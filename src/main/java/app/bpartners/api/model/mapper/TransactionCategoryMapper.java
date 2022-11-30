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

import static app.bpartners.api.service.utils.FractionUtils.parseFraction;

@Component
@AllArgsConstructor
public class TransactionCategoryMapper {
  private final TransactionCategoryTemplateJpaRepository templateJpaRepository;
  private final TransactionCategoryJpaRepository jpaRepository;

  public TransactionCategory toDomain(HTransactionCategory entity, LocalDate startDate,
                                      LocalDate endDate) {
    if (entity == null) {
      return null;
    }
    TransactionCategory domain = TransactionCategory.builder()
        .id(entity.getId())
        .idAccount(entity.getIdAccount())
        .type(entity.getType())
        .vat(parseFraction(entity.getVat()))
        .idTransactionCategoryTmpl(entity.getIdCategoryTemplate())
        .build();
    if (!entity.isUserDefined()) {
      HTransactionCategoryTemplate categoryTemplate =
          templateJpaRepository.getById(entity.getIdCategoryTemplate());
      domain.setTransactionType(categoryTemplate.getTransactionType());
      domain.setType(categoryTemplate.getType());
      domain.setVat(parseFraction(categoryTemplate.getVat()));
    }
    String typeOrIdCategoryTmpl =
        entity.getType() != null ? entity.getType() : entity.getIdCategoryTemplate();
    Long typeCount = jpaRepository.countByCriteria(domain.getIdAccount(), typeOrIdCategoryTmpl,
        startDate.atStartOfDay(),
        endDate.plusDays(1).atStartOfDay().minusSeconds(1));
    domain.setTypeCount(typeCount);
    return domain;
  }

  public TransactionCategory toDomain(
      String idAccount, HTransactionCategory entity,
      LocalDate startDate, LocalDate endDate) {
    String idCategoryTemplate = entity.getIdCategoryTemplate();
    if (entity.isUserDefined()) {
      return TransactionCategory.builder()
          .id(entity.getId())
          .idAccount(entity.getIdAccount())
          .type(entity.getType())
          .vat(parseFraction(entity.getVat()))
          .idTransactionCategoryTmpl(entity.getIdCategoryTemplate())
          .typeCount(
              getCategoryCount(entity.getIdAccount(), startDate, endDate, entity.getType()))
          //TODO: when it's a user defined category, user should give transaction type
          .transactionType(null)
          .build();
    }
    HTransactionCategoryTemplate categoryTemplate =
        templateJpaRepository.getById(idCategoryTemplate);
    return TransactionCategory.builder()
        .id(entity.getId())
        .idAccount(idAccount)
        .type(categoryTemplate.getType())
        .vat(parseFraction(categoryTemplate.getVat()))
        .idTransactionCategoryTmpl(idCategoryTemplate)
        .transactionType(categoryTemplate.getTransactionType())
        .typeCount(getCategoryCount(idAccount, startDate, endDate, idCategoryTemplate))
        .build();
  }

  public TransactionCategoryTemplate toDomain(HTransactionCategoryTemplate entity) {
    if (entity == null) {
      return null;
    }
    return TransactionCategoryTemplate.builder()
        .id(entity.getId())
        .type(entity.getType())
        .vat(parseFraction(entity.getVat()))
        .build();
  }

  private long getCategoryCount(String idAccount, LocalDate startDate, LocalDate endDate,
                                String idCategoryTemplate) {
    return jpaRepository.countByCriteria(idAccount, idCategoryTemplate,
        startDate.atStartOfDay(),
        endDate.plusDays(1).atStartOfDay().minusSeconds(1));
  }

  public HTransactionCategory toEntity(TransactionCategory category) {
    return HTransactionCategory.builder()
        .id(category.getId())
        .idAccount(category.getIdAccount())
        .type(category.getType())
        .idTransaction(category.getIdTransaction())
        .idCategoryTemplate(category.getIdTransactionCategoryTmpl())
        .vat(category.getVat().toString())
        .build();
  }

  public HTransactionCategory toEntity(HTransactionCategoryTemplate template) {
    return HTransactionCategory.builder()
        .id(null)
        .idTransaction(null)
        .idAccount(null)
        .idCategoryTemplate(template.getId())
        .type(template.getType())
        .vat(template.getVat())
        .build();
  }
}
