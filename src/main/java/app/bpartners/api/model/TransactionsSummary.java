package app.bpartners.api.model;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class TransactionsSummary {
  private int year;

  private List<MonthlyTransactionsSummary> summary;

  public Integer getAnnualIncome() {
    AtomicReference<Integer> income = new AtomicReference<>(0);
    summary.forEach(monthly -> income.set(income.get() + monthly.getIncome().getCentsRoundUp()));
    return income.get();
  }

  public Integer getAnnualOutcome() {
    AtomicReference<Integer> outcome = new AtomicReference<>(0);
    summary.forEach(monthly -> outcome.set(outcome.get() + monthly.getOutcome().getCentsRoundUp()));
    return outcome.get();
  }

  public Integer getAnnualCashFlow() {
    return summary.isEmpty() ? 0
        : summary.get(summary.size() - 1).getCashFlow().getCentsRoundUp();
  }
}
