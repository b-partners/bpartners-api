package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.TransactionCategory;
import app.bpartners.api.model.mapper.TransactionCategoryMapper;
import app.bpartners.api.repository.TransactionCategoryRepository;
import app.bpartners.api.repository.jpa.TransactionCategoryJpaRepository;
import app.bpartners.api.repository.jpa.model.HTransactionCategory;
import java.time.LocalDate;
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

import static app.bpartners.api.repository.jpa.model.HTransactionCategory.ID_ACCOUNT_ATTRIBUTE;
import static app.bpartners.api.repository.jpa.model.HTransactionCategory.ID_CATEGORY_TMPL_ATTRIBUTE;
import static app.bpartners.api.repository.jpa.model.HTransactionCategory.TYPE_ATTRIBUTE;
import static app.bpartners.api.repository.jpa.model.HTransactionCategory.VAT_ATTRIBUTE;

@Repository
@AllArgsConstructor
public class TransactionCategoryRepositoryImpl implements TransactionCategoryRepository {
  private static final LocalDate INITIAL_DATE = LocalDate.of(2000, 1, 1);
  private final TransactionCategoryJpaRepository jpaRepository;
  private final TransactionCategoryMapper domainMapper;
  private final EntityManager entityManager;

  //TODO: rename the from/to variables to be more explicit
  @Override
  public List<TransactionCategory> findByIdAccountAndUserDefined(
      String idAccount, boolean unique, boolean userDefined, LocalDate from,
      LocalDate to) {
    List<HTransactionCategory> entities;
    if (unique) {
      entities = findByCriteriaOrderByCreatedDatetime(idAccount, userDefined);
    } else {
      entities = findAllByIdAccountAndUserDefined(idAccount, userDefined);
    }
    if (entities.isEmpty()) {
      return List.of();
    }
    return entities.stream()
        .map(e -> domainMapper.toDomain(e, from, to))
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public List<TransactionCategory> findByAccount(String idAccount, boolean unique, LocalDate from,
                                                 LocalDate to) {
    if (unique) {
      return findByCriteriaOrderByCreatedDatetime(idAccount).stream()
          .map(e -> domainMapper.toDomain(e, from, to))
          .collect(Collectors.toUnmodifiableList());
    }
    return jpaRepository.findAllByIdAccount(idAccount).stream()
        .map(e -> domainMapper.toDomain(e, from, to))
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public List<TransactionCategory> saveAll(List<TransactionCategory> toCreate) {
    List<HTransactionCategory> entitiesToCreate = toCreate.stream()
        .map(domainMapper::toEntity)
        .map(this::userDefinedCheck)
        .collect(Collectors.toUnmodifiableList());
    return jpaRepository.saveAll(entitiesToCreate).stream()
        .map(e -> domainMapper.toDomain(e, INITIAL_DATE, LocalDate.now()))
        .collect(Collectors.toUnmodifiableList());
  }

  private List<HTransactionCategory> findDistinctByAccount(String idAccount) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<HTransactionCategory> query = builder.createQuery(HTransactionCategory.class);
    Root<HTransactionCategory> root = query.from(HTransactionCategory.class);

    Predicate hasIdAccount = builder.equal(root.get(ID_ACCOUNT_ATTRIBUTE), idAccount);
    query.where(builder.and(hasIdAccount));
    query.multiselect(transactionCategorySelections(root)).distinct(true);

    return entityManager.createQuery(query).getResultList();
  }

  @Override
  public TransactionCategory findByIdTransaction(String idTransaction) {
    Optional<HTransactionCategory> entity =
        jpaRepository.findFirstByCreatedDatetimeAndIdTransaction(idTransaction);
    if (entity.isEmpty()) {
      return null;
    }
    return domainMapper.toDomain(entity.get(), INITIAL_DATE, LocalDate.now());
  }

  private List<HTransactionCategory> findAllByIdAccountAndUserDefined(
      String idAccount,
      boolean userDefined) {
    return entityManager.createQuery(retrieveCriteriaQuery(idAccount, userDefined))
        .getResultList();
  }

  private CriteriaQuery<HTransactionCategory> retrieveCriteriaQuery(
      String idAccount, boolean userDefined) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<HTransactionCategory> query = builder.createQuery(HTransactionCategory.class);
    Root<HTransactionCategory> root = query.from(HTransactionCategory.class);

    Predicate hasIdAccount = builder.equal(root.get(ID_ACCOUNT_ATTRIBUTE), idAccount);
    Predicate isUserDefined = builder.isNotNull(root.get(ID_CATEGORY_TMPL_ATTRIBUTE));
    if (userDefined) {
      isUserDefined = builder.isNull(root.get(ID_CATEGORY_TMPL_ATTRIBUTE));
    }
    query.where(builder.and(hasIdAccount, isUserDefined));

    return query;
  }

  private List<HTransactionCategory> findDistinctByCriteria(
      String idAccount, boolean userDefined) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<HTransactionCategory> query = builder.createQuery(HTransactionCategory.class);
    Root<HTransactionCategory> root = query.from(HTransactionCategory.class);

    Predicate hasIdAccount = builder.equal(root.get(ID_ACCOUNT_ATTRIBUTE), idAccount);
    Predicate isUserDefined = builder.isNotNull(root.get(ID_CATEGORY_TMPL_ATTRIBUTE));
    if (userDefined) {
      isUserDefined = builder.isNull(root.get(ID_CATEGORY_TMPL_ATTRIBUTE));
    }
    query.where(builder.and(hasIdAccount, isUserDefined));
    query.multiselect(transactionCategorySelections(root)).distinct(true);

    return entityManager.createQuery(query).getResultList();
  }

  public List<HTransactionCategory> findByCriteriaOrderByCreatedDatetime(
      String idAccount, boolean userDefined) {
    return findDistinctByCriteria(idAccount, userDefined).stream()
        .map(c -> {
          if (!userDefined) {
            return jpaRepository.findByCriteriaOrderByCreatedDatetime(c.getIdAccount(),
                c.getIdCategoryTemplate());
          }
          return jpaRepository.findByCriteriaOrderByCreatedDatetime(idAccount, c.getType(),
              c.getVat());
        })
        .collect(Collectors.toUnmodifiableList());
  }

  public List<HTransactionCategory> findByCriteriaOrderByCreatedDatetime(
      String idAccount) {
    return findDistinctByAccount(idAccount).stream()
        .map(c -> {
          if (!c.isUserDefined()) {
            return jpaRepository.findByCriteriaOrderByCreatedDatetime(c.getIdAccount(),
                c.getIdCategoryTemplate());
          }
          return jpaRepository.findByCriteriaOrderByCreatedDatetime(idAccount, c.getType(),
              c.getVat());
        })
        .collect(Collectors.toUnmodifiableList());
  }

  private List<Selection<?>> transactionCategorySelections(Root<HTransactionCategory> root) {
    return List.of(root.get(ID_ACCOUNT_ATTRIBUTE), root.get(ID_CATEGORY_TMPL_ATTRIBUTE),
        root.get(TYPE_ATTRIBUTE),
        root.get(VAT_ATTRIBUTE));
  }

  private HTransactionCategory userDefinedCheck(HTransactionCategory toCheck) {
    if (!toCheck.isUserDefined()) {
      toCheck.setType(null);
      toCheck.setVat(null);
    }
    return toCheck;
  }
}
