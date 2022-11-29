package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.TransactionsSummary;
import app.bpartners.api.model.mapper.TransactionsSummaryMapper;
import app.bpartners.api.repository.TransactionsSummaryRepository;
import app.bpartners.api.repository.jpa.TransactionsSummaryJpaRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class TransactionsSummaryRepositoryImpl implements TransactionsSummaryRepository {
  private final TransactionsSummaryJpaRepository jpaRepository;

  private final TransactionsSummaryMapper mapper;

  @Override
  public TransactionsSummary getByAccountIdAndYear(String accountId, int year) {
    return mapper.toDomain(year, jpaRepository.getByIdAccountAndYear(accountId, year));
  }
}
