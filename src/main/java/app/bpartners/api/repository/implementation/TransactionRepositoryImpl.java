package app.bpartners.api.repository.implementation;

import app.bpartners.api.endpoint.rest.model.TransactionTypeEnum;
import app.bpartners.api.model.Transaction;
import app.bpartners.api.model.TransactionCategory;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.TransactionMapper;
import app.bpartners.api.repository.TransactionCategoryRepository;
import app.bpartners.api.repository.TransactionRepository;
import app.bpartners.api.repository.jpa.TransactionJpaRepository;
import app.bpartners.api.repository.jpa.model.HTransaction;
import app.bpartners.api.repository.swan.TransactionSwanRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class TransactionRepositoryImpl implements TransactionRepository {
  private final TransactionSwanRepository swanRepository;
  private final TransactionMapper mapper;
  private final TransactionCategoryRepository categoryRepository;
  private final TransactionJpaRepository jpaRepository;

  @Override
  public List<Transaction> findByAccountId(String id) {
    return swanRepository.getTransactions().stream()
        .map(swanTransaction -> mapper.toDomain(
            swanTransaction,
            categoryRepository.findByIdTransaction(swanTransaction.getNode().getId()),
            getOrCreateBySwanId(swanTransaction.getNode().getId())
        ))
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public Transaction updateType(String swanTransactionId, TransactionTypeEnum type) {
    HTransaction persisted = getBySwanId(swanTransactionId);
    persisted.setType(type);
    return mapper.toDomain(
        swanRepository.findById(persisted.getSwanId()),
        categoryRepository.findByIdTransaction(persisted.getSwanId()),
        persisted
    );
  }

  public Transaction findById(String id) {
    HTransaction entity = getBySwanId(id);
    app.bpartners.api.repository.swan.model.Transaction transaction =
        swanRepository.findById(id);
    TransactionCategory category = categoryRepository.findByIdTransaction(id);
    return mapper.toDomain(transaction, category, entity);
  }

  private HTransaction getBySwanId(String swanId) {
    return jpaRepository.findBySwanId(swanId)
        .orElseThrow(
            () -> new NotFoundException(
                "Transaction." + swanId + " not found.")
        );
  }

  private HTransaction getOrCreateBySwanId(String swanId) {
    return jpaRepository.findBySwanId(swanId)
        .orElseGet(() ->
            jpaRepository.save(HTransaction.builder()
                .swanId(swanId)
                .build()));
  }
}