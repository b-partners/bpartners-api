package app.bpartners.api.repository.implementation;

import app.bpartners.api.endpoint.rest.model.TransactionStatus;
import app.bpartners.api.model.Transaction;
import app.bpartners.api.model.mapper.TransactionMapper;
import app.bpartners.api.repository.TransactionCategoryRepository;
import app.bpartners.api.repository.TransactionRepository;
import app.bpartners.api.repository.swan.TransactionSwanRepository;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
@AllArgsConstructor
public class TransactionRepositoryImpl implements TransactionRepository {
  private final TransactionSwanRepository swanRepository;
  private final TransactionMapper mapper;
  private final TransactionCategoryRepository categoryRepository;

  @Override
  public List<Transaction> findByAccountId(String accountId) {
    return swanRepository.getByIdAccount(accountId)
        .stream()
        .map(transaction -> mapper.toDomain(transaction,
            categoryRepository.findByIdTransaction(transaction.getNode().getId())))
        .collect(Collectors.toUnmodifiableList());
  }

  @Override
  public List<Transaction> findByAccountIdAndStatus(String id, TransactionStatus status) {
    return findByAccountId(id).stream()
        .filter(transaction -> transaction.getStatus().equals(status))
        .collect(Collectors.toUnmodifiableList());
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
}
