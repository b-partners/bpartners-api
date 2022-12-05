package app.bpartners.api.model;

import java.time.Instant;
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
public class MonthlyTransactionsSummary {
  private String id;
  private int month;
  private Fraction income;
  private Fraction outcome;
  private Fraction cashFlow;
  private Instant updatedAt;
}
