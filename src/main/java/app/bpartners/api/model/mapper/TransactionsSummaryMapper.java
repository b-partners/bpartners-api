package app.bpartners.api.model.mapper;

import static app.bpartners.api.service.utils.FractionUtils.parseFraction;

import app.bpartners.api.model.MonthlyTransactionsSummary;
import app.bpartners.api.model.TransactionsSummary;
import app.bpartners.api.repository.jpa.model.HMonthlyTransactionsSummary;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class TransactionsSummaryMapper {
  public TransactionsSummary toDomain(int year, List<HMonthlyTransactionsSummary> summaries) {
    return TransactionsSummary.builder()
        .year(year)
        .summary(summaries.stream().map(this::toDomain).toList())
        .build();
  }

  public MonthlyTransactionsSummary toDomain(HMonthlyTransactionsSummary entity) {
    if (entity == null) {
      return null;
    }
    return MonthlyTransactionsSummary.builder()
        .id(entity.getId())
        .month(entity.getMonth())
        .cashFlow(parseFraction(entity.getCashFlow()))
        .income(parseFraction(entity.getIncome()))
        .outcome(parseFraction(entity.getOutcome()))
        .updatedAt(entity.getUpdatedAt())
        .build();
  }
}
