package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.MonthlyTransactionsSummary;
import app.bpartners.api.model.TransactionsSummary;
import app.bpartners.api.model.mapper.TransactionsSummaryMapper;
import app.bpartners.api.repository.TransactionsSummaryRepository;
import app.bpartners.api.repository.jpa.TransactionsSummaryJpaRepository;
import app.bpartners.api.repository.jpa.model.HMonthlyTransactionsSummary;
import java.time.Instant;
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

  public MonthlyTransactionsSummary updateYearMonthSummary(
      String accountId, int year,
      MonthlyTransactionsSummary monthlySummary) {
    HMonthlyTransactionsSummary persisted =
        jpaRepository.getByIdAccountAndYearAndMonth(accountId, year, monthlySummary.getMonth());
    if (persisted == null) {
      persisted = HMonthlyTransactionsSummary.builder()
          .idAccount(accountId)
          .month(monthlySummary.getMonth())
          .year(year)
          .build();
    }
    persisted.setIncome(monthlySummary.getIncome().toString());
    persisted.setCashFlow(monthlySummary.getCashFlow().toString());
    persisted.setOutcome(monthlySummary.getOutcome().toString());
    persisted.setUpdatedAt(Instant.now());

    return mapper.toDomain(jpaRepository.save(persisted));
  }

  @Override
  public MonthlyTransactionsSummary getByAccountIdAndYearMonth(
      String accountId, int year, int month) {
    return mapper.toDomain(jpaRepository.getByIdAccountAndYearAndMonth(accountId, year, month));
  }
}
