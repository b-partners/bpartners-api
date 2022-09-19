package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.Account;
import app.bpartners.api.model.Transaction;
import app.bpartners.api.model.exception.ForbiddenException;
import app.bpartners.api.model.mapper.TransactionMapper;
import app.bpartners.api.repository.AccountRepository;
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
  private final AccountRepository accountRepository;
  private final TransactionCategoryRepository categoryRepository;

  @Override
  public List<Transaction> findByAccountId(String id) {
    Account authenticatedAccount = accountRepository.findAll().get(0);
    if (!id.equals(authenticatedAccount.getId())) {
      throw new ForbiddenException();
    }
    //TODO: replace this with persisted ID of TransactionCategory
    return swanRepository.getTransactions().stream()
        .map(transaction -> mapper.toDomain(transaction,
            categoryRepository.findByIdTransaction(transaction.getNode().getId())))
        .collect(Collectors.toUnmodifiableList());
  }
}
