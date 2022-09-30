package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.Product;
import app.bpartners.api.model.mapper.ProductMapper;
import app.bpartners.api.repository.ProductRepository;
import app.bpartners.api.repository.jpa.InvoiceProductJpaRepository;
import app.bpartners.api.repository.jpa.ProductJpaRepository;
import app.bpartners.api.repository.jpa.model.HInvoiceProduct;
import app.bpartners.api.repository.jpa.model.HProduct;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Selection;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

import static app.bpartners.api.repository.jpa.model.HProduct.DESCRIPTION_ATTRIBUTE;
import static app.bpartners.api.repository.jpa.model.HTransactionCategory.ID_ACCOUNT_ATTRIBUTE;

@Repository
@AllArgsConstructor
public class ProductRepositoryImpl implements ProductRepository {
  private final ProductJpaRepository jpaRepository;
  private final ProductMapper domainMapper;
  private final EntityManager entityManager;
  private final InvoiceProductJpaRepository ipJpaRepository;

  @Override
  public List<Product> findByIdAccount(String idAccount, boolean unique) {
    if (unique) {
      return findDistinctByCriteriaOrderByDate(idAccount).stream()
          .map(domainMapper::toDomain)
          .collect(Collectors.toUnmodifiableList());
    }
    return jpaRepository.findAllByIdAccount(idAccount).stream()
        .map(domainMapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public List<Product> findByIdAccountAndDescription(String idAccount, String description) {
    return findDistinctByCriteriaOrderByDate(idAccount, description).stream()
        .map(domainMapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public List<Product> saveAll(String idAccount, List<Product> toSave) {
    List<HProduct> entitiesToSave = toSave.stream()
        .map(product -> domainMapper.toEntity(idAccount, product))
        .collect(Collectors.toUnmodifiableList());
    return jpaRepository.saveAll(entitiesToSave).stream()
        .map(domainMapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public List<Product> findByIdInvoice(String idInvoice) {
    HInvoiceProduct invoiceProduct = ipJpaRepository
        .findTopByIdInvoiceOrderByCreatedDatetimeDesc(idInvoice);
    if (invoiceProduct == null) {
      return List.of();
    }
    return invoiceProduct.getProducts().stream()
        .map(domainMapper::toDomain)
        .collect(Collectors.toUnmodifiableList());
  }


  List<HProduct> findDistinctByAccountAndDescription(String idAccount, String description) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<HProduct> query = builder.createQuery(HProduct.class);
    Root<HProduct> root = query.from(HProduct.class);

    Predicate hasIdAccount = builder.equal(root.get(ID_ACCOUNT_ATTRIBUTE), idAccount);
    Predicate containsDescription =
        builder.or(
            builder.like(
                root.get(DESCRIPTION_ATTRIBUTE), "%" + description + "%"
            ),
            builder.like(
                builder.lower(
                    root.get((DESCRIPTION_ATTRIBUTE)
                    )
                ), "%" + description + "%"
            ));
    query.where(builder.and(hasIdAccount, containsDescription));
    query.multiselect(productSelections(root)).distinct(true);

    return entityManager.createQuery(query).getResultList();
  }

  private List<HProduct> findDistinctByAccount(String idAccount) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<HProduct> query = builder.createQuery(HProduct.class);
    Root<HProduct> root = query.from(HProduct.class);

    Predicate hasIdAccount = builder.equal(root.get(ID_ACCOUNT_ATTRIBUTE), idAccount);

    query.where(builder.and(hasIdAccount));
    query.multiselect(productSelections(root)).distinct(true);

    return entityManager.createQuery(query).getResultList();
  }

  private List<HProduct> findDistinctByCriteriaOrderByDate(String idAccount) {
    return findDistinctByAccount(idAccount).stream()
        .map(product -> jpaRepository.findDistinctByCriteriaOrderByDate(product.getDescription()))
        .collect(Collectors.toUnmodifiableList());
  }

  private List<HProduct> findDistinctByCriteriaOrderByDate(String idAccount, String description) {
    return findDistinctByAccountAndDescription(idAccount, description).stream()
        .map(product -> jpaRepository.findDistinctByCriteriaOrderByDate(product.getDescription()))
        .collect(Collectors.toUnmodifiableList());
  }


  private List<Selection<?>> productSelections(Root<HProduct> root) {
    return List.of(root.get(DESCRIPTION_ATTRIBUTE));
  }
}
