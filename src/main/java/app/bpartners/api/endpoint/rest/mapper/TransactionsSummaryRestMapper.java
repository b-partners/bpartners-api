package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.MonthlyTransactionsSummary;
import app.bpartners.api.endpoint.rest.model.TransactionsSummary;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class TransactionsSummaryRestMapper {
  public TransactionsSummary toRest(app.bpartners.api.model.TransactionsSummary domain) {
    return new TransactionsSummary()
        .year(domain.getYear())
        .summary(domain.getSummary().stream()
            .map(this::toRest)
            .collect(Collectors.toUnmodifiableList()));
  }

  public MonthlyTransactionsSummary toRest(
      app.bpartners.api.model.MonthlyTransactionsSummary domain) {
    return new MonthlyTransactionsSummary()
        .id(domain.getId())
        .month(domain.getMonth())
        .updatedAt(domain.getUpdatedAt())
        .income(domain.getIncome().getCents())
        .outcome(domain.getOutcome().getCents())
        .cashFlow(domain.getCashFlow().getCents());
  }
}
