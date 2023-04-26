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
      String accountId, int year, MonthlyTransactionsSummary monthlySummary) {
    HMonthlyTransactionsSummary summaryEntity =
        jpaRepository.findByIdAccountAndYearAndMonth(accountId, year, monthlySummary.getMonth())
            .orElse(HMonthlyTransactionsSummary.builder()
                .idAccount(accountId)
                .month(monthlySummary.getMonth())
                .year(year)
                .build());
    HMonthlyTransactionsSummary toSave = summaryEntity.toBuilder()
        .cashFlow(monthlySummary.getCashFlow().toString())
        .income(monthlySummary.getIncome().toString())
        .outcome(monthlySummary.getOutcome().toString())
        .updatedAt(Instant.now())
        .build();
    return mapper.toDomain(jpaRepository.save(toSave));
  }

  @Override
  public MonthlyTransactionsSummary getByAccountIdAndYearMonth(
      String accountId, int year, int month) {
    return mapper.toDomain(jpaRepository.getByIdAccountAndYearAndMonth(accountId, year, month));
  }
}
