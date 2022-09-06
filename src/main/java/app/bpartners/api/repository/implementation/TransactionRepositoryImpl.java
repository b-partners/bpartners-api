package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.Transaction;
import app.bpartners.api.model.TransactionCategory;
import app.bpartners.api.model.mapper.TransactionMapper;
import app.bpartners.api.repository.TransactionRepository;
import app.bpartners.api.repository.jpa.model.HTransactionCategory;
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

  @Override
  public List<Transaction> findByAccountId(String id) {
    TransactionCategory domainCategory = mapper.toDomain(category1Mock());
    return swanRepository.getTransactions().stream()
        .map(transaction -> mapper.toDomain(transaction, domainCategory))
        .collect(Collectors.toUnmodifiableList());
  }

  HTransactionCategory category1Mock() {
    return HTransactionCategory.builder()
        .id("category1_id")
        .label("label1")
        .build();
  }
}
