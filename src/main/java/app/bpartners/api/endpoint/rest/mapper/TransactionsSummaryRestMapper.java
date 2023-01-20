package app.bpartners.api.endpoint.rest.mapper;

import app.bpartners.api.endpoint.rest.model.MonthlyTransactionsSummary;
import app.bpartners.api.endpoint.rest.model.TransactionsSummary;
import java.time.Instant;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class TransactionsSummaryRestMapper {
  public TransactionsSummary toRest(app.bpartners.api.model.TransactionsSummary domain) {
    return new TransactionsSummary()
        .year(domain.getYear())
        .annualIncome(domain.getAnnualIncome())
        .annualOutcome(domain.getAnnualOutcome())
        .annualCashFlow(domain.getAnnualCashFlow())
        .updatedAt(getLastInstantUpdate(domain))
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
        .income(domain.getIncome().getCentsRoundUp())
        .outcome(domain.getOutcome().getCentsRoundUp())
        .cashFlow(domain.getCashFlow().getCentsRoundUp());
  }

  private static Instant getLastInstantUpdate(app.bpartners.api.model.TransactionsSummary domain) {
    AtomicReference<Instant> lastUpdate = new AtomicReference<>();
    domain.getSummary().stream()
        .max(Comparator.comparing(
            app.bpartners.api.model.MonthlyTransactionsSummary::getUpdatedAt))
        .ifPresent(
            monthlyTransactionsSummary ->
                lastUpdate.set(monthlyTransactionsSummary.getUpdatedAt()));
    return lastUpdate.get();
  }
}
