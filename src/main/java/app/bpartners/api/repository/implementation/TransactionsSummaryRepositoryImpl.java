package app.bpartners.api.repository.implementation;

import app.bpartners.api.model.MonthlyTransactionsSummary;
import app.bpartners.api.model.TransactionsSummary;
import app.bpartners.api.model.mapper.TransactionsSummaryMapper;
import app.bpartners.api.repository.TransactionsSummaryRepository;
import app.bpartners.api.repository.jpa.TransactionsSummaryJpaRepository;
import app.bpartners.api.repository.jpa.model.HMonthlyTransactionsSummary;
import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class TransactionsSummaryRepositoryImpl implements TransactionsSummaryRepository {
  private final TransactionsSummaryJpaRepository jpaRepository;
  private final TransactionsSummaryMapper mapper;

  @Override
  public TransactionsSummary getByIdUserAndYear(String idUser, int year) {
    List<HMonthlyTransactionsSummary> monthlySummaries =
        jpaRepository.getByIdUserAndYear(idUser, year);
    return mapper.toDomain(year, monthlySummaries);
  }

  @Override
  public TransactionsSummary getByAccountHolderIdAndYear(String accountHolderId, int year) {
    List<HMonthlyTransactionsSummary> monthlySummaries =
        jpaRepository.getByIdAccountHolderIdAndYear(accountHolderId, year);
    return mapper.toDomain(year, monthlySummaries);
  }

  public MonthlyTransactionsSummary updateYearMonthSummary(
      String idUser, int year, MonthlyTransactionsSummary monthlySummary) {
    HMonthlyTransactionsSummary summaryEntity =
        jpaRepository.findByIdUserAndYearAndMonth(idUser, year, monthlySummary.getMonth())
            .orElse(HMonthlyTransactionsSummary.builder()
                .idUser(idUser)
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
  public MonthlyTransactionsSummary getByIdUserAndYearMonth(
      String idUser, int year, int month) {
    return mapper.toDomain(jpaRepository.getByIdUserAndYearAndMonth(idUser, year, month));
  }

  @Override
  public void removeAll(String userId) {
    List<HMonthlyTransactionsSummary> toRemove = jpaRepository.getByIdUser(userId);
    jpaRepository.deleteAll(toRemove);
  }
}
