package app.bpartners.api.repository.implementation;

import app.bpartners.api.endpoint.rest.model.TransactionTypeEnum;
import app.bpartners.api.model.Account;
import app.bpartners.api.model.Transaction;
import app.bpartners.api.model.exception.ForbiddenException;
import app.bpartners.api.model.exception.NotFoundException;
import app.bpartners.api.model.mapper.TransactionMapper;
import app.bpartners.api.repository.AccountRepository;
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
  private final AccountRepository accountRepository;
  private final TransactionCategoryRepository categoryRepository;
  private final TransactionJpaRepository jpaRepository;

  @Override
  public List<Transaction> findByAccountId(String id) {
    Account authenticatedAccount = accountRepository.findAll().get(0);
    if (!id.equals(authenticatedAccount.getId())) {
      throw new ForbiddenException();
    }
    //TODO: replace this with persisted ID of TransactionCategory
    return swanRepository.getTransactions().stream()
        .map(transaction -> mapper.toDomain(
            transaction,
            categoryRepository.findByIdTransaction(transaction.getNode().getId()),
            getBySwanId(transaction.getNode().getId())
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

  public Transaction findBySwanId(String swanTransactionId) {
    return mapper.toDomain(
        swanRepository.findById(swanTransactionId),
        categoryRepository.findByIdTransaction(swanTransactionId),
        getBySwanId(swanTransactionId)
    );
  }

  private HTransaction getBySwanId(String swanId) {
    return jpaRepository.findBySwanId(swanId)
        .orElseThrow(
            () -> new NotFoundException(
                "Transaction." + swanId + " not found.")
        );
  }
}