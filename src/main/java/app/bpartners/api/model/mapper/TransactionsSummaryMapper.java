package app.bpartners.api.model.mapper;

import app.bpartners.api.model.MonthlyTransactionsSummary;
import app.bpartners.api.model.TransactionsSummary;
import app.bpartners.api.repository.jpa.model.HMonthlyTransactionsSummary;
import java.util.List;
import org.springframework.stereotype.Component;

import static app.bpartners.api.service.utils.FractionUtils.parseFraction;

@Component
public class TransactionsSummaryMapper {
  public TransactionsSummary toDomainFromEntity(int year, List<HMonthlyTransactionsSummary> summaries) {
    return TransactionsSummary.builder()
        .year(year)
        .summary(summaries.stream()
            .map(this::toDomain)
            .toList())
        .build();
  }

  public TransactionsSummary toDomain(int year, List<MonthlyTransactionsSummary> summaries) {
    return TransactionsSummary.builder()
        .year(year)
        .summary(summaries)
        .build();
  }


  public MonthlyTransactionsSummary toDomain(HMonthlyTransactionsSummary entity) {
    if (entity == null) {
      return null;
    }
    return MonthlyTransactionsSummary.builder()
        .id(entity.getId())
        .month(entity.getMonth())
        .year(entity.getYear())
        .cashFlow(parseFraction(entity.getCashFlow()))
        .income(parseFraction(entity.getIncome()))
        .outcome(parseFraction(entity.getOutcome()))
        .transactionSummaryStatus(entity.getTransactionSummaryStatus())
        .updatedAt(entity.getUpdatedAt())
        .build();
  }
}
