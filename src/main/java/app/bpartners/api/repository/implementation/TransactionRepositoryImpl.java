package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.Transaction;
import app.bpartners.api.model.mapper.TransactionMapper;
import app.bpartners.api.repository.TransactionCategoryRepository;
import app.bpartners.api.repository.TransactionRepository;
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

  @Override
  public List<Transaction> findByAccountId(String id) {
    //TODO : replace this with persisted ID of TransactionCategory
    return swanRepository.getTransactions().stream()
        .map(transaction -> mapper.toDomain(transaction,
            categoryRepository.findByIdTransaction(transaction.node.id)))
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public Transaction findById(String id) {
    app.bpartners.api.repository.swan.model.Transaction swanTransaction =
        swanRepository.findById(id);
    return mapper.toDomain(swanTransaction,
        categoryRepository.findByIdTransaction(swanTransaction.node.id));
  }
}
