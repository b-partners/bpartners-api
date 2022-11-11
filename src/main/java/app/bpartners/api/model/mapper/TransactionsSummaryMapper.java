package app.bpartners.api.model.mapper;

import app.bpartners.api.model.MonthlyTransactionsSummary;
import app.bpartners.api.model.TransactionsSummary;
import app.bpartners.api.repository.jpa.model.HMonthlyTransactionsSummary;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

import static app.bpartners.api.service.utils.FractionUtils.parseFraction;

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
