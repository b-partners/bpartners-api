package app.bpartners.api.repository.implementation;

import app.bpartners.api.endpoint.rest.model.EnableStatus;
import app.bpartners.api.model.MonthlyTransactionsSummary;
import app.bpartners.api.model.TransactionsSummary;
import app.bpartners.api.model.mapper.TransactionsSummaryMapper;
import app.bpartners.api.repository.TransactionsSummaryRepository;
import app.bpartners.api.repository.jpa.TransactionsSummaryJpaRepository;
import app.bpartners.api.repository.jpa.model.HMonthlyTransactionsSummary;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

import static app.bpartners.api.endpoint.rest.model.EnableStatus.ENABLED;

@Repository
@AllArgsConstructor
public class TransactionsSummaryRepositoryImpl implements TransactionsSummaryRepository {
  private final TransactionsSummaryJpaRepository jpaRepository;
  private final TransactionsSummaryMapper mapper;

  @Override
  public List<TransactionsSummary> getByIdUser(String idUser) {
    List<HMonthlyTransactionsSummary> toRemove = jpaRepository.getByIdUser(idUser);
    List<MonthlyTransactionsSummary> allSummaries = toRemove.stream()
        .map(mapper::toDomain)
        .toList();

    return mapToTransactionsSummary(allSummaries);
  }

  private List<TransactionsSummary> mapToTransactionsSummary(
      List<MonthlyTransactionsSummary> allSummaries) {
    Map<Integer, List<MonthlyTransactionsSummary>> monthlySummariesByYear = new HashMap<>();
    for (MonthlyTransactionsSummary s : allSummaries) {
      int year = s.getYear();
      if (!monthlySummariesByYear.containsKey(year)) {
        List<MonthlyTransactionsSummary> subList = new ArrayList<>();
        subList.add(s);
        monthlySummariesByYear.put(year, subList);
      } else {
        monthlySummariesByYear.get(year).add(s);
      }
    }
    List<TransactionsSummary> transactionsSummaries = new ArrayList<>();
    monthlySummariesByYear.forEach(
        (year, summaries) -> {
          transactionsSummaries.add(mapper.toDomain(year, summaries));
        }
    );
    return transactionsSummaries;
  }

  @Override
  public List<TransactionsSummary> saveAll(List<TransactionsSummary> toSave) {
    List<HMonthlyTransactionsSummary> monthlyEntities = new ArrayList<>();
    for (TransactionsSummary ts : toSave) {
      String idUser = ts.getIdUser();
      int year = ts.getYear();
      List<MonthlyTransactionsSummary> summariesToSave = ts.getSummary();
      for (MonthlyTransactionsSummary mts : summariesToSave) {
        HMonthlyTransactionsSummary summaryEntity =
            jpaRepository.findByIdUserAndYearAndMonth(idUser, year, mts.getMonth())
                .orElse(HMonthlyTransactionsSummary.builder()
                    .idUser(idUser)
                    .month(mts.getMonth())
                    .year(year)
                    .build());
        HMonthlyTransactionsSummary toSaveEntity = summaryEntity.toBuilder()
            .cashFlow(mts.getCashFlow().toString())
            .income(mts.getIncome().toString())
            .outcome(mts.getOutcome().toString())
            .transactionSummaryStatus(mts.getTransactionSummaryStatus())
            .updatedAt(Instant.now())
            .build();

        monthlyEntities.add(toSaveEntity);
      }
    }
    List<MonthlyTransactionsSummary> savedSummaries =
        jpaRepository.saveAll(monthlyEntities).stream()
            .map(mapper::toDomain)
            .toList();
    return mapToTransactionsSummary(savedSummaries);
  }

  public TransactionsSummary getByIdUserAndYearAndStatus(String idUser, int year,
                                                         EnableStatus status) {
    List<HMonthlyTransactionsSummary> monthlySummaries =
        jpaRepository.getByIdUserAndYearAndTransactionSummaryStatus(idUser, year, status);
    return mapper.toDomainFromEntity(year, monthlySummaries);
  }

  @Override
  public TransactionsSummary getEnabledByAccountHolderIdAndYear(String accountHolderId, int year) {
    List<HMonthlyTransactionsSummary> monthlySummaries =
        jpaRepository.getByIdAccountHolderIdAndYearAndStatus(
            accountHolderId,
            year,
            ENABLED.getValue());
    return mapper.toDomainFromEntity(year, monthlySummaries);
  }

  public MonthlyTransactionsSummary updateYearMonthSummary(
      String idUser, int year, MonthlyTransactionsSummary monthlySummary) {
    HMonthlyTransactionsSummary summaryEntity =
        jpaRepository.findByIdUserAndYearAndMonth(idUser, year, monthlySummary.getMonth())
            .orElse(HMonthlyTransactionsSummary.builder()
                .idUser(idUser)
                .month(monthlySummary.getMonth())
                .transactionSummaryStatus(ENABLED)
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
  public MonthlyTransactionsSummary getEnabledByIdUserAndYearMonth(
      String idUser, int year, int month) {
    return mapper.toDomain(
        jpaRepository.getByIdUserAndYearAndMonthAndTransactionSummaryStatus(idUser,
            year, month,
            ENABLED));
  }

  @Override
  public void removeAll(String userId) {
    List<HMonthlyTransactionsSummary> toRemove = jpaRepository.getByIdUser(userId);
    jpaRepository.deleteAll(toRemove);
  }
}
