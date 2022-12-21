package app.bpartners.api.model.mapper;

import app.bpartners.api.model.TransactionCategory;
import app.bpartners.api.model.TransactionCategoryTemplate;
import app.bpartners.api.repository.jpa.TransactionCategoryJpaRepository;
import app.bpartners.api.repository.jpa.TransactionCategoryTemplateJpaRepository;
import app.bpartners.api.repository.jpa.model.HTransactionCategory;
import app.bpartners.api.repository.jpa.model.HTransactionCategoryTemplate;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
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
        .comment(entity.getComment())
        .build();
    HTransactionCategoryTemplate categoryTemplate =
        templateJpaRepository.getById(entity.getIdCategoryTemplate());
    domain.setTransactionType(categoryTemplate.getTransactionType());
    domain.setType(categoryTemplate.getType());
    domain.setVat(parseFraction(categoryTemplate.getVat()));
    domain.setDescription(categoryTemplate.getDescription());
    domain.setOther(categoryTemplate.isOther());
    long typeCount = getCategoryCount(
        entity.getIdAccount(), startDate, endDate, domain.getType()
    );
    domain.setTypeCount(typeCount);
    return domain;
  }

  public TransactionCategory toDomain(
      String idAccount, HTransactionCategory entity,
      LocalDate startDate, LocalDate endDate) {
    String idCategoryTemplate = entity.getIdCategoryTemplate();
    HTransactionCategoryTemplate categoryTemplate =
        templateJpaRepository.getById(idCategoryTemplate);
    return TransactionCategory.builder()
        .id(entity.getId())
        .idAccount(idAccount)
        .type(categoryTemplate.getType())
        .vat(parseFraction(categoryTemplate.getVat()))
        .idTransactionCategoryTmpl(idCategoryTemplate)
        .other(categoryTemplate.isOther())
        .transactionType(categoryTemplate.getTransactionType())
        .typeCount(getCategoryCount(idAccount, startDate, endDate, entity.getType()))
        .description(categoryTemplate.getDescription())
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
        .transactionType(entity.getTransactionType())
        .other(entity.isOther())
        .description(entity.getDescription())
        .build();
  }

  private long getCategoryCount(String idAccount, LocalDate startDate, LocalDate endDate,
                                String type) {
    //TODO: a better count would consider type and comment
    return jpaRepository.countByCriteria(
        idAccount,
        type,
        startDate
            .atStartOfDay()
            .truncatedTo(ChronoUnit.DAYS)
            .toInstant(ZoneOffset.UTC),
        endDate
            .atStartOfDay()
            .truncatedTo(ChronoUnit.DAYS)
            .toInstant(ZoneOffset.UTC)
            .plus(1, ChronoUnit.DAYS)
            .minusSeconds(1));
  }

  public HTransactionCategory toEntity(TransactionCategory category) {
    return HTransactionCategory.builder()
        .id(category.getId())
        .idAccount(category.getIdAccount())
        .type(category.getType())
        .idTransaction(category.getIdTransaction())
        .idCategoryTemplate(category.getIdTransactionCategoryTmpl())
        .vat(category.getVat().toString())
        .comment(category.getComment())
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
        .comment(null)
        .description(template.getDescription())
        .build();
  }
}
