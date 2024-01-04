package app.bpartners.api.model;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@Builder(toBuilder = true)
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

  @Getter(AccessLevel.NONE)
  private Instant updatedAt;

  public Instant getUpdatedAt() {
    if (updatedAt == null) {
      return null;
    }
    return updatedAt.truncatedTo(ChronoUnit.MILLIS);
  }
}
