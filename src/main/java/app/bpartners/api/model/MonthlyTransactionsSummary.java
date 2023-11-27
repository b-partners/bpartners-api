package app.bpartners.api.model;

import app.bpartners.api.endpoint.rest.model.EnableStatus;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.Type;

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
  private int year;
  private Fraction income;
  private Fraction outcome;
  private Fraction cashFlow;
  @Getter(AccessLevel.NONE)
  private Instant updatedAt;
  @Type(type = "pgsql_enum")
  @Enumerated(EnumType.STRING)
  private EnableStatus transactionSummaryStatus;

  public Instant getUpdatedAt() {
    if (updatedAt == null) {
      return null;
    }
    return updatedAt.truncatedTo(ChronoUnit.MILLIS);
  }
}
