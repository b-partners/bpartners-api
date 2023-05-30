package app.bpartners.api.model.mapper;

import app.bpartners.api.model.MonthlyTransactionsSummary;
import app.bpartners.api.model.TransactionsSummary;
import app.bpartners.api.repository.jpa.model.HMonthlyTransactionsSummary;
import app.bpartners.api.service.utils.MoneyUtils;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class TransactionsSummaryMapper {
  public TransactionsSummary toDomain(int year, List<HMonthlyTransactionsSummary> summaries) {
    return TransactionsSummary.builder()
        .year(year)
        .summary(summaries.stream()
            .map(this::toDomain)
            .collect(Collectors.toUnmodifiableList()))
        .build();
  }


  public MonthlyTransactionsSummary toDomain(HMonthlyTransactionsSummary entity) {
    if (entity == null) {
      return null;
    }
    return MonthlyTransactionsSummary.builder()
        .id(entity.getId())
        .month(entity.getMonth())
        .cashFlow(MoneyUtils.fromMinorString(entity.getCashFlow()))
        .income(MoneyUtils.fromMinorString(entity.getIncome()))
        .outcome(MoneyUtils.fromMinorString(entity.getOutcome()))
        .updatedAt(entity.getUpdatedAt())
        .build();
  }
}
