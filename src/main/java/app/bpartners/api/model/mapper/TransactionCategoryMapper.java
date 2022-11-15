package app.bpartners.api.model.mapper;

import app.bpartners.api.model.TransactionCategory;
import app.bpartners.api.model.TransactionCategoryTemplate;
import app.bpartners.api.repository.jpa.TransactionCategoryJpaRepository;
import app.bpartners.api.repository.jpa.TransactionCategoryTemplateJpaRepository;
import app.bpartners.api.repository.jpa.model.HTransactionCategory;
import app.bpartners.api.repository.jpa.model.HTransactionCategoryTemplate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

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
        .idTransactionCategoryTmpl(entity.getIdCategoryTemplate())
        .comment(entity.getComment())
        .description(entity.getDescription())
        .build();
    templateJpaRepository.findById(entity.getIdCategoryTemplate())
        .ifPresent(template -> domain.setTransactionType(template.getTransactionType()));
    //TODO: change the LocalDatetime to Instant because the Instant does not aware timezone
    LocalDateTime startDatetime = startDate.atStartOfDay();
    LocalDateTime endDatetime = endDate.plusDays(1).atStartOfDay().minusSeconds(1);
    Long typeCount = jpaRepository.countByCriteria(domain.getIdAccount(), entity.getType(),
        startDatetime, endDatetime);
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
        .isOther(entity.isOther())
        .transactionType(entity.getTransactionType())
        .build();
  }

  public HTransactionCategory toEntity(TransactionCategory category) {
    return HTransactionCategory.builder()
        .id(category.getId())
        .idAccount(category.getIdAccount())
        .idTransaction(category.getIdTransaction())
        .idCategoryTemplate(category.getIdTransactionCategoryTmpl())
        .type(category.getType())
        .comment(category.getComment())
        .description(category.getDescription())
        .build();
  }
}
