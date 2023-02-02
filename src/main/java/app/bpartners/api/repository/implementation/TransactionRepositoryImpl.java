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
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Slf4j
@AllArgsConstructor
public class TransactionRepositoryImpl implements TransactionRepository {
  private final TransactionSwanRepository swanRepository;
  private final TransactionMapper mapper;
  private final TransactionCategoryRepository categoryRepository;
  private TransactionJpaRepository jpaRepository;

  @Transactional(isolation = Isolation.SERIALIZABLE)
  @Override
  public List<Transaction> findByAccountId(String accountId) {
    return swanRepository.getByIdAccount(accountId)
        .stream()
        .map(transaction -> {
          HTransaction entity = getOrCreateTransaction(accountId, transaction);
          return mapper.toDomain(transaction, entity,
              categoryRepository.findByIdTransaction(entity.getId()));
        })
        .collect(Collectors.toUnmodifiableList());
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
    return jpaRepository
        .findByIdSwan(transaction.getNode().getId())
        .orElseGet(() -> jpaRepository.save(mapper.toEntity(accountId, transaction)));
  }
}
