package app.bpartners.api.repository.implementation;

import app.bpartners.api.endpoint.rest.model.TransactionStatus;
import app.bpartners.api.model.JustifyTransaction;
import app.bpartners.api.model.Transaction;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.TransactionMapper;
import app.bpartners.api.repository.TransactionCategoryRepository;
import app.bpartners.api.repository.TransactionRepository;
import app.bpartners.api.repository.connectors.transaction.TransactionConnector;
import app.bpartners.api.repository.connectors.transaction.TransactionConnectorRepository;
import app.bpartners.api.repository.jpa.InvoiceJpaRepository;
import app.bpartners.api.repository.jpa.TransactionJpaRepository;
import app.bpartners.api.repository.jpa.model.HInvoice;
import app.bpartners.api.repository.jpa.model.HTransaction;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import static app.bpartners.api.service.utils.TransactionUtils.describeList;

@Repository
@Slf4j
@AllArgsConstructor
public class TransactionRepositoryImpl implements TransactionRepository {
  private final TransactionMapper mapper;
  private final TransactionCategoryRepository categoryRepository;
  private final TransactionJpaRepository jpaRepository;
  private final InvoiceJpaRepository invoiceJpaRepository;
  private final TransactionConnectorRepository connectorRepository;

  @Override
  public List<Transaction> findByIdAccount(String idAccount, String label, int page, int pageSize) {
    Pageable pageable = PageRequest.of(page, pageSize);
    List<HTransaction> transactions =
        jpaRepository.findByIdAccountAndLabelContainingIgnoreCaseOrderByPaymentDateTimeDesc(
            idAccount, label, pageable);
    return transactions
        .stream()
        .map(transaction -> mapper.toDomain(transaction,
            categoryRepository.findByIdTransaction(transaction.getId())))
        .collect(Collectors.toList());
  }

  //TODO: check why transactions with same bridge ID are persisted twice
  @Override
  public List<Transaction> findPersistedByIdAccount(String idAccount, int page, int pageSize) {
    Pageable pageable = PageRequest.of(page, pageSize);
    return jpaRepository.findByIdAccountOrderByPaymentDateTimeDesc(idAccount, pageable).stream()
        .map(transaction -> mapper.toDomain(transaction,
            categoryRepository.findByIdTransaction(transaction.getId())))
        .collect(Collectors.toList());
  }

  @Override
  public List<Transaction> findByAccountId(String idAccount) {
    List<TransactionConnector> connectors = connectorRepository.findByIdAccount(idAccount);
    List<HTransaction> entities = connectors.stream()
        .map(connector -> {
          List<HTransaction> bridgeTransactions =
              jpaRepository.findAllByIdBridge(Long.valueOf(connector.getId()));
          if (bridgeTransactions.isEmpty()) {
            throw new NotFoundException(
                "Transaction(externalId=" + connector.getId() + ") not found");
          }
          if (bridgeTransactions.size() > 1) {
            log.warn("Duplicated transactions with same external ID {}",
                describeList(bridgeTransactions));
          }
          return bridgeTransactions.get(0);
        })
        .collect(Collectors.toList());
    return entities.stream()
        .map(entity -> mapper.toDomain(entity,
            categoryRepository.findByIdTransaction(entity.getId())))
        //TODO: when getting from database only, sort by payment date DESC directly in db query
        .sorted(Comparator.comparing(Transaction::getPaymentDatetime).reversed())
        .collect(Collectors.toList());
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
    return findByAccountId(id).stream()
        .filter(transaction -> transaction.getStatus().equals(status))
        .collect(Collectors.toUnmodifiableList());
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
  public List<Transaction> findByAccountIdAndStatusBetweenInstants(
      String id, TransactionStatus status,
      Instant from, Instant to) {
    return findByAccountIdAndStatus(id, status).stream()
        .filter(
            transaction -> transaction.getPaymentDatetime().isAfter(from)
                &&
                transaction.getPaymentDatetime().isBefore(to)
        )
        .collect(Collectors.toUnmodifiableList());
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
