package app.bpartners.api.repository;

import static java.util.UUID.randomUUID;

import app.bpartners.api.endpoint.rest.model.EnableStatus;
import app.bpartners.api.endpoint.rest.model.TransactionStatus;
import app.bpartners.api.model.FileInfo;
import app.bpartners.api.model.JustifyTransaction;
import app.bpartners.api.model.Transaction;
import app.bpartners.api.model.TransactionInvoiceDetails;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.TransactionMapper;
import app.bpartners.api.model.mapper.TransactionSupportingDocsMapper;
import app.bpartners.api.repository.jpa.FileInfoJpaRepository;
import app.bpartners.api.repository.jpa.InvoiceJpaRepository;
import app.bpartners.api.repository.jpa.TransactionJpaRepository;
import app.bpartners.api.repository.jpa.TransactionSupportingDocsJpaRepository;
import app.bpartners.api.repository.jpa.model.HFileInfo;
import app.bpartners.api.repository.jpa.model.HInvoice;
import app.bpartners.api.repository.jpa.model.HTransaction;
import app.bpartners.api.repository.jpa.model.HTransactionSupportingDocs;
import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.stereotype.Repository;

@Primary
@Repository
@AllArgsConstructor
public class DbTransactionRepository implements TransactionRepository {
  private final TransactionMapper mapper;
  private final TransactionCategoryRepository categoryRepository;
  private final TransactionJpaRepository jpaRepository;
  private final InvoiceJpaRepository invoiceJpaRepository;
  private final FileInfoJpaRepository fileInfoJpaRepository;
  private final TransactionSupportingDocsJpaRepository transactionDocsRep;
  private final TransactionSupportingDocsMapper transactionDocsMapper;
  private final EntityManager entityManager;

  private List<HTransaction> filterByIdAccountAndLabel(
      String idAccount, String label, TransactionStatus status, Pageable pageable) {
    CriteriaBuilder builder = entityManager.getCriteriaBuilder();
    CriteriaQuery<HTransaction> query = builder.createQuery(HTransaction.class);
    Root<HTransaction> root = query.from(HTransaction.class);

    List<Predicate> predicates = new ArrayList<>();
    predicates.add(builder.equal(root.get("idAccount"), idAccount));
    predicates.add(builder.equal(root.get("enableStatus"), EnableStatus.ENABLED));
    if (label != null) {
      predicates.add(
          builder.or(
              builder.like(builder.lower(root.get("label")), "%" + label.toLowerCase() + "%")));
    }
    if (status != null) {
      predicates.add(builder.or(builder.equal(root.get("status"), status)));
    }
    query
        .where(builder.and(predicates.toArray(new Predicate[0])))
        .orderBy(QueryUtils.toOrders(pageable.getSort(), root, builder));

    return entityManager
        .createQuery(query)
        .setFirstResult((pageable.getPageNumber()) * pageable.getPageSize())
        .setMaxResults(pageable.getPageSize())
        .getResultList();
  }

  @Override
  public List<Transaction> findByIdAccount(
      String idAccount, String label, TransactionStatus status, int page, int pageSize) {
    Pageable pageable =
        PageRequest.of(page, pageSize, Sort.by(Sort.Direction.DESC, "paymentDateTime"));
    List<HTransaction> transactions = filterByIdAccountAndLabel(idAccount, label, status, pageable);
    return transactions.stream()
        .map(
            transaction ->
                mapper.toDomain(
                    transaction,
                    categoryRepository.findByIdTransaction(transaction.getId()),
                    transactionDocsRep.findAllByIdTransaction(transaction.getId()).stream()
                        .map(transactionDocsMapper::toDomain)
                        .toList()))
        .collect(Collectors.toList());
  }

  // TODO: Bad implementation ! Use correct SQL Query
  @Override
  public List<Transaction> findByAccountId(String id) {
    return jpaRepository.findAllByIdAccountOrderByPaymentDateTimeDesc(id).stream()
        .map(
            transaction ->
                mapper.toDomain(
                    transaction,
                    categoryRepository.findByIdTransaction(transaction.getId()),
                    transactionDocsRep.findAllByIdTransaction(transaction.getId()).stream()
                        .map(transactionDocsMapper::toDomain)
                        .toList()))
        .toList();
  }

  @Override
  public Transaction findById(String id) {
    HTransaction transaction =
        jpaRepository
            .findById(id)
            .orElseThrow(() -> new NotFoundException("Transaction." + id + " is not found."));
    return mapper.toDomain(
        transaction,
        categoryRepository.findByIdTransaction(transaction.getId()),
        transactionDocsRep.findAllByIdTransaction(transaction.getId()).stream()
            .map(transactionDocsMapper::toDomain)
            .toList());
  }

  // TODO: Bad implementation ! Use correct SQL Query
  @Override
  public List<Transaction> findByAccountIdAndStatus(String id, TransactionStatus status) {
    return findByAccountId(id).stream()
        .filter(transaction -> transaction.getStatus().equals(status))
        .toList();
  }

  @Override
  public Transaction save(JustifyTransaction toSave) {
    HTransaction entity =
        jpaRepository
            .findById(toSave.getIdTransaction())
            .orElseThrow(
                () ->
                    new NotFoundException(
                        "Transaction(id=" + toSave.getIdTransaction() + ") not found"));
    HInvoice invoice =
        invoiceJpaRepository
            .findById(toSave.getIdInvoice())
            .orElseThrow(
                () -> new NotFoundException("Invoice(id=" + toSave.getIdInvoice() + ") not found"));
    HTransaction savedTransaction = jpaRepository.save(entity.toBuilder().invoice(invoice).build());
    return mapper.toDomain(
        savedTransaction,
        categoryRepository.findByIdTransaction(entity.getId()),
        transactionDocsRep.findAllByIdTransaction(entity.getId()).stream()
            .map(transactionDocsMapper::toDomain)
            .toList());
  }

  @Override
  public List<Transaction> saveAll(List<Transaction> transactions) {
    List<HTransactionSupportingDocs> supportingDocsList = new ArrayList<>();
    List<HTransaction> entities =
        transactions.stream()
            .map(
                transaction -> {
                  TransactionInvoiceDetails invoiceDetails = transaction.getInvoiceDetails();
                  HInvoice invoice =
                      invoiceDetails == null || invoiceDetails.getIdInvoice() == null
                          ? null
                          : invoiceJpaRepository.getById(invoiceDetails.getIdInvoice());
                  /*
                  TODO: historize by ENABLE and DISABLE status instead of replacing old*/
                  List<HTransactionSupportingDocs> newSuppDocs = computeSupportingDocs(transaction);
                  List<HTransactionSupportingDocs> existingSuppDocs =
                      transactionDocsRep.findAllByIdTransaction(transaction.getId());
                  List<String> existingDocsIds =
                      existingSuppDocs.stream().map(HTransactionSupportingDocs::getId).toList();
                  List<String> newDocsIds =
                      newSuppDocs.stream().map(HTransactionSupportingDocs::getId).toList();
                  // If new supporting documents are to be set
                  if (!new HashSet<>(existingDocsIds).containsAll(newDocsIds)) {
                    transactionDocsRep.deleteAllById(
                        existingSuppDocs.stream().map(HTransactionSupportingDocs::getId).toList());
                    supportingDocsList.addAll(newSuppDocs);
                  }
                  return mapper.toEntity(transaction, invoice);
                })
            .toList();

    List<HTransaction> savedTransactions = jpaRepository.saveAll(entities);
    transactionDocsRep.saveAll(supportingDocsList);

    return savedTransactions.stream()
        .map(
            entity ->
                mapper.toDomain(
                    entity,
                    categoryRepository.findByIdTransaction(entity.getId()),
                    transactionDocsRep.findAllByIdTransaction(entity.getId()).stream()
                        .map(transactionDocsMapper::toDomain)
                        .toList()))
        .collect(Collectors.toList());
  }

  // TODO: Bad implementation ! Use correct SQL Query
  @Override
  public List<Transaction> findByAccountIdAndStatusBetweenInstants(
      String id, TransactionStatus status, Instant from, Instant to) {
    return findByAccountIdAndStatus(id, status).stream()
        .filter(
            transaction ->
                transaction.getPaymentDatetime().isAfter(from)
                    && transaction.getPaymentDatetime().isBefore(to))
        .toList();
  }

  @Override
  public Transaction getById(String idTransaction) {
    HTransaction entity =
        jpaRepository
            .findById(idTransaction)
            .orElseThrow(
                () -> new NotFoundException("Transaction." + idTransaction + " not found"));
    return mapper.toDomain(
        entity,
        categoryRepository.findByIdTransaction(entity.getId()),
        transactionDocsRep.findAllByIdTransaction(entity.getId()).stream()
            .map(transactionDocsMapper::toDomain)
            .toList());
  }

  @Override
  public void removeAll(List<Transaction> toRemove) {
    List<String> ids = new ArrayList<>();
    toRemove.forEach(transaction -> ids.add(transaction.getId()));
    jpaRepository.deleteAllById(ids);
  }

  private List<HTransactionSupportingDocs> computeSupportingDocs(Transaction transaction) {
    return transaction.getSupportingDocuments().stream()
        .map(
            newDocs -> {
              FileInfo fileInfo = newDocs.getFileInfo();
              HFileInfo fileInfoEntity = fileInfoJpaRepository.getById(fileInfo.getId());
              return HTransactionSupportingDocs.builder()
                  .id(newDocs.getId() == null ? String.valueOf(randomUUID()) : newDocs.getId())
                  .idTransaction(transaction.getId())
                  .fileInfo(fileInfoEntity)
                  .build();
            })
        .toList();
  }
}
