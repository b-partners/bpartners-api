package app.bpartners.api.repository.implementation;

import app.bpartners.api.endpoint.rest.model.TransactionStatus;
import app.bpartners.api.model.Transaction;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.TransactionMapper;
import app.bpartners.api.repository.TransactionCategoryRepository;
import app.bpartners.api.repository.TransactionRepository;
import app.bpartners.api.repository.jpa.TransactionJpaRepository;
import app.bpartners.api.repository.jpa.model.HTransaction;
import app.bpartners.api.repository.swan.TransactionSwanRepository;
import app.bpartners.api.repository.swan.model.Transaction.Node;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import static app.bpartners.api.model.mapper.TransactionMapper.getTransactionStatus;

@Repository
@Slf4j
@AllArgsConstructor
public class TransactionRepositoryImpl implements TransactionRepository {
  private final TransactionSwanRepository swanRepository;
  private final TransactionMapper mapper;
  private final TransactionCategoryRepository categoryRepository;
  private TransactionJpaRepository jpaRepository;

  @Override
  public List<Transaction> findByAccountId(String accountId) {
    List<Transaction> transactionsFromSwan = swanRepository.getByIdAccount(accountId)
        .stream()
        .map(transaction -> {
          HTransaction entity = getOrCreateTransaction(accountId, transaction);
          return mapper.toDomain(transaction, entity,
              categoryRepository.findByIdTransaction(entity.getId()));
        })
        .collect(Collectors.toUnmodifiableList());
    if (transactionsFromSwan.isEmpty()) {
      List<HTransaction> transactions = jpaRepository.findAllByIdAccount(accountId);
      return transactions.stream()
          .map(transaction ->
              mapper.toDomain(transaction,
                  categoryRepository.findByIdTransaction(transaction.getId())))
          .collect(Collectors.toList());
    }
    return transactionsFromSwan;
  }

  @Override
  public Transaction findByAccountIdAndId(String accountId, String transactionId) {
    Optional<HTransaction> optionalEntity = jpaRepository.findById(transactionId);
    if (optionalEntity.isEmpty()) {
      throw new NotFoundException("Transaction." + transactionId + " not found.");
    }
    HTransaction persisted = optionalEntity.get();
    return mapper.toDomain(
        swanRepository.findById(persisted.getIdSwan()),
        persisted,
        categoryRepository.findByIdTransaction(transactionId)
    );
  }

  @Override
  public List<Transaction> findByAccountIdAndStatus(String id, TransactionStatus status) {
    return findByAccountId(id).stream()
        .filter(transaction -> transaction.getStatus().equals(status))
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public Transaction save(Transaction toSave) {
    HTransaction entity = jpaRepository.save(mapper.toEntity(toSave));
    app.bpartners.api.repository.swan.model.Transaction
        swanTransaction = swanRepository.findById(entity.getIdSwan());
    return mapper.toDomain(swanTransaction, entity,
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
    app.bpartners.api.repository.swan.model.Transaction
        swanTransaction = swanRepository.findById(entity.getIdSwan());
    return mapper.toDomain(swanTransaction, entity,
        categoryRepository.findByIdTransaction(entity.getId()));
  }

  private HTransaction getOrCreateTransaction(
      String accountId,
      app.bpartners.api.repository.swan.model.Transaction transaction) {
    Optional<HTransaction> optional = jpaRepository.findByIdSwan(transaction.getNode().getId());
    if (optional.isPresent()) {
      HTransaction optionalValue = optional.get();
      checkTransactionUpdates(transaction.getNode(), optionalValue);
      return jpaRepository.save(optionalValue);
    }
    return jpaRepository.save(mapper.toEntity(accountId, transaction));
  }

  private static void checkTransactionUpdates(Node transactionNode, HTransaction optionalValue) {
    if (optionalValue.getAmount() != null
        && !optionalValue.getAmount().equals(transactionNode.getAmount().getValue())) {
      optionalValue.setAmount(transactionNode.getAmount().getValue());
    }
    if (optionalValue.getCurrency() != null
        && !optionalValue.getCurrency().equals(transactionNode.getAmount().getCurrency())) {
      optionalValue.setCurrency(transactionNode.getAmount().getCurrency());
    }
    if (optionalValue.getStatus() != null
        && !optionalValue.getStatus().getValue()
        .equals(transactionNode.getStatusInfo().getStatus())) {
      optionalValue.setStatus(getTransactionStatus(transactionNode.getStatusInfo().getStatus()));
    }
    if (optionalValue.getPaymentDateTime() != null
        && !optionalValue.getPaymentDateTime().equals(transactionNode.getCreatedAt())) {
      optionalValue.setPaymentDateTime(transactionNode.getCreatedAt());
    }
    if (optionalValue.getLabel() != null
        && !optionalValue.getLabel().equals((transactionNode.getLabel()))) {
      optionalValue.setLabel(transactionNode.getLabel());
    }
    if (optionalValue.getReference() != null
        && !optionalValue.getReference().equals(transactionNode.getReference())) {
      optionalValue.setReference(transactionNode.getReference());
    }
    if (optionalValue.getSide() != null
        && !optionalValue.getSide().equals(transactionNode.getSide())) {
      optionalValue.setSide(transactionNode.getSide());
    }
  }
}
