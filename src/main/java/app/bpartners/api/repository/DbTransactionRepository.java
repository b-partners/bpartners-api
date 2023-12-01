package app.bpartners.api.repository;

import app.bpartners.api.endpoint.rest.model.EnableStatus;
import app.bpartners.api.endpoint.rest.model.TransactionStatus;
import app.bpartners.api.model.JustifyTransaction;
import app.bpartners.api.model.Transaction;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.exception.NotImplementedException;
import app.bpartners.api.model.mapper.TransactionMapper;
import app.bpartners.api.repository.jpa.InvoiceJpaRepository;
import app.bpartners.api.repository.jpa.TransactionJpaRepository;
import app.bpartners.api.repository.jpa.model.HInvoice;
import app.bpartners.api.repository.jpa.model.HTransaction;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class DbTransactionRepository implements TransactionRepository {
  private final TransactionMapper mapper;
  private final TransactionCategoryRepository categoryRepository;
  private final TransactionJpaRepository jpaRepository;
  private final InvoiceJpaRepository invoiceJpaRepository;
  private final EntityManager entityManager;

  private List<HTransaction> filterByIdAccountAndLabel(String idAccount, String label,
                                                       TransactionStatus status,
                                                       Pageable pageable) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<HTransaction> query = builder.createQuery(HTransaction.class);
    Root<HTransaction> root = query.from(HTransaction.class);

    List<Predicate> predicates = new ArrayList<>();
    predicates.add(builder.equal(root.get("idAccount"), idAccount));
    predicates.add(builder.equal(root.get("enableStatus"), EnableStatus.ENABLED));
    if (label != null) {
      predicates.add(builder.or(builder.like(builder.lower(root.get("label")),
          "%" + label.toLowerCase() + "%")));
    }
    if (status != null) {
      predicates.add(builder.or(builder.equal(root.get("status"), status)));
    }
    query
        .where(builder.and(predicates.toArray(new Predicate[0])))
        .orderBy(QueryUtils.toOrders(pageable.getSort(), root, builder));

    return entityManager.createQuery(query)
        .setFirstResult((pageable.getPageNumber()) * pageable.getPageSize())
        .setMaxResults(pageable.getPageSize())
        .getResultList();
  }

  @Override
  public List<Transaction> findByIdAccount(String idAccount,
                                           String label,
                                           TransactionStatus status,
                                           int page, int pageSize) {
    Pageable pageable =
        PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "paymentDateTime"));
    List<HTransaction> transactions = filterByIdAccountAndLabel(idAccount, label, status, pageable);
    return transactions
        .stream()
        .map(transaction -> mapper.toDomain(transaction,
            categoryRepository.findByIdTransaction(transaction.getId())))
        .collect(Collectors.toList());
  }

  @Override
  public List<Transaction> findByAccountId(String id) {
    throw new NotFoundException("Not supported ! Must be pageable");
  }

  @Override
  public Transaction findById(String id) {
    HTransaction transaction = jpaRepository.findById(id)
        .orElseThrow(() -> new NotFoundException("Transaction." + id + " is not found."));
    return mapper.toDomain(
        transaction, categoryRepository.findByIdTransaction(transaction.getId()));
  }

  @Override
  public List<Transaction> findByAccountIdAndStatus(String id, TransactionStatus status) {
    throw new NotImplementedException("Not supported");
  }

  @Override
  public Transaction save(JustifyTransaction toSave) {
    HTransaction entity = jpaRepository.findById(toSave.getIdTransaction())
        .orElseThrow(() -> new NotFoundException("Transaction(id=" + toSave.getIdTransaction()
            + ") not found"));
    HInvoice invoice = invoiceJpaRepository.findById(toSave.getIdInvoice())
        .orElseThrow(() -> new NotFoundException("Invoice(id=" + toSave.getIdInvoice()
            + ") not found"));
    HTransaction savedTransaction = jpaRepository.save(entity.toBuilder()
        .invoice(invoice)
        .build());
    return mapper.toDomain(savedTransaction,
        categoryRepository.findByIdTransaction(entity.getId()));
  }

  @Override
  public List<Transaction> saveAll(List<Transaction> transactions) {
    List<HTransaction> entities = transactions.stream()
        .map(mapper::toEntity)
        .toList();
    ;
    return entities.stream()
        .map(entity -> mapper.toDomain(entity,
            categoryRepository.findByIdTransaction(entity.getId())))
        .collect(Collectors.toList());
  }

  @Override
  public List<Transaction> findByAccountIdAndStatusBetweenInstants(String id,
                                                                   TransactionStatus status,
                                                                   Instant from, Instant to) {
    throw new NotImplementedException("Not supported");
  }

  @Override
  public Transaction getById(String idTransaction) {
    HTransaction entity = jpaRepository.findById(idTransaction)
        .orElseThrow(() -> new NotFoundException(
            "Transaction." + idTransaction + " not found"));
    return mapper.toDomain(entity, categoryRepository.findByIdTransaction(entity.getId()));
  }

  @Override
  public void removeAll(List<Transaction> toRemove) {
    List<String> ids = new ArrayList<>();
    toRemove.forEach(
        transaction -> ids.add(transaction.getId())
    );
    jpaRepository.deleteAllById(ids);
  }
}