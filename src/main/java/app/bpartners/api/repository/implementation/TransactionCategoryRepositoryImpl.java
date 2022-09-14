package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.TransactionCategory;
import app.bpartners.api.model.mapper.TransactionCategoryMapper;
import app.bpartners.api.repository.TransactionCategoryRepository;
import app.bpartners.api.repository.jpa.TransactionCategoryJpaRepository;
import app.bpartners.api.repository.jpa.model.HTransactionCategory;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class TransactionCategoryRepositoryImpl implements TransactionCategoryRepository {
  private final TransactionCategoryJpaRepository jpaRepository;
  private final TransactionCategoryMapper domainMapper;
  private final EntityManager entityManager;

  @Override
  public List<TransactionCategory> findByIdAccount(
      String idAccount, boolean unique,
      boolean userDefined) {
    List<HTransactionCategory> entities;
    if (unique) {
      entities = findDistinctByIdAccountAndUserDefined(idAccount, userDefined);
    } else {
      entities = jpaRepository.findAllByIdAccountAndUserDefined(idAccount, userDefined);
    }
    if (entities.isEmpty()) {
      return List.of();
    }
    return entities.stream()
        .map(domainMapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public List<TransactionCategory> saveAll(List<TransactionCategory> toCreate) {
    List<HTransactionCategory> entitiesToCreate = toCreate.stream()
        .map(domainMapper::toEntity)
        .collect(Collectors.toUnmodifiableList());
    return jpaRepository.saveAll(entitiesToCreate).stream()
        .map(domainMapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public TransactionCategory findByIdTransaction(String idTransaction) {
    Optional<HTransactionCategory> entity =
        jpaRepository.findFirstByOrderByTypeAndIdTransaction(idTransaction);
    if (entity.isEmpty()) {
      return null;
    }
    return domainMapper.toDomain(entity.get());
  }

  public List<HTransactionCategory> findDistinctByIdAccountAndUserDefined(
      String idAccount,
      boolean userDefined) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<HTransactionCategory> query = builder.createQuery(HTransactionCategory.class);
    Root<HTransactionCategory> root = query.from(HTransactionCategory.class);

    Predicate hasIdAccount = builder.equal(root.get("idAccount"), idAccount);
    Predicate isUserDefined = builder.equal(root.get("userDefined"), userDefined);
    query.where(builder.and(hasIdAccount, isUserDefined));
    query.multiselect(transactionCategorySelections(root)).distinct(true);

    return entityManager.createQuery(query).getResultList();
  }

  private List<Selection<?>> transactionCategorySelections(Root<HTransactionCategory> root) {
    return List.of(root.get("idTransaction"), root.get("idAccount"), root.get("type"), root.get(
        "vat"), root.get("userDefined"));
  }
}
