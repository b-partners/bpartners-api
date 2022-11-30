package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.TransactionCategory;
import app.bpartners.api.model.mapper.TransactionCategoryMapper;
import app.bpartners.api.repository.TransactionCategoryRepository;
import app.bpartners.api.repository.jpa.TransactionCategoryJpaRepository;
import app.bpartners.api.repository.jpa.TransactionCategoryTemplateJpaRepository;
import app.bpartners.api.repository.jpa.model.HTransactionCategory;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Order;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

import static app.bpartners.api.repository.jpa.model.HTransactionCategory.CREATED_DATETIME_ATTRIBUTE;
import static app.bpartners.api.repository.jpa.model.HTransactionCategory.ID_ACCOUNT_ATTRIBUTE;
import static app.bpartners.api.repository.jpa.model.HTransactionCategory.ID_CATEGORY_TMPL_ATTRIBUTE;
import static app.bpartners.api.repository.jpa.model.HTransactionCategory.TYPE_ATTRIBUTE;
import static app.bpartners.api.repository.jpa.model.HTransactionCategory.VAT_ATTRIBUTE;

@Repository
@AllArgsConstructor
public class TransactionCategoryRepositoryImpl implements TransactionCategoryRepository {
  private static final LocalDate DEFAULT_START_DATE = LocalDate.of(2000, 1, 1);
  private final TransactionCategoryJpaRepository jpaRepository;
  private final TransactionCategoryTemplateJpaRepository templateJpaRepository;
  private final TransactionCategoryMapper domainMapper;
  private final EntityManager entityManager;

  @Override
  public List<TransactionCategory> findByIdAccount(
      String idAccount,
      LocalDate startDate, LocalDate endDate) {
    List<HTransactionCategory> entities =
        findAllByCriteria(idAccount);
    if (entities.isEmpty()) {
      return List.of();
    }
    return entities.stream()
        .map(entity -> domainMapper.toDomain(idAccount, entity, startDate, endDate))
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public List<TransactionCategory> saveAll(List<TransactionCategory> toCreate) {
    List<HTransactionCategory> entitiesToCreate = toCreate.stream()
        .map(domainMapper::toEntity)
        .collect(Collectors.toUnmodifiableList());
    return jpaRepository.saveAll(entitiesToCreate).stream()
        .map(category -> domainMapper.toDomain(category, DEFAULT_START_DATE, LocalDate.now()))
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public TransactionCategory findByIdTransaction(String idTransaction) {
    HTransactionCategory entity =
        jpaRepository.findTopByIdTransactionOrderByCreatedDatetimeDesc(idTransaction);
    return domainMapper.toDomain(entity, DEFAULT_START_DATE, LocalDate.now());
  }


  private List<HTransactionCategory> findDistinctByCriteria(
      String idAccount) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<HTransactionCategory> query = builder.createQuery(HTransactionCategory.class);
    Root<HTransactionCategory> root = query.from(HTransactionCategory.class);

    Predicate hasIdAccount = builder.equal(root.get(ID_ACCOUNT_ATTRIBUTE), idAccount);

    query.where(builder.and(hasIdAccount));
    query.multiselect(transactionCategorySelections(root)).distinct(true);

    return entityManager.createQuery(query).getResultList();
  }

  public List<HTransactionCategory> findAllByCriteria(
      String idAccount) {
    List<HTransactionCategory> categories = new ArrayList<>();
    categories.addAll(findDistinctByCriteria(idAccount));
    categories.addAll(templateJpaRepository.findAll().stream()
        .map(domainMapper::toEntity)
        .collect(Collectors.toUnmodifiableList()));
    return categories;
  }

  private List<Selection<?>> transactionCategorySelections(Root<HTransactionCategory> root) {
    return List.of(
        root.get(ID_ACCOUNT_ATTRIBUTE),
        root.get(ID_CATEGORY_TMPL_ATTRIBUTE),
        root.get(TYPE_ATTRIBUTE),
        root.get(VAT_ATTRIBUTE));
  }
}
