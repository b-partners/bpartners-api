package app.bpartners.api.model;

import app.bpartners.api.repository.jpa.model.HAccountHolder;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class AnnualRevenueTarget {
  private String id;
  private Integer year;
  private HAccountHolder accountHolder;
  private Fraction amountTarget;
  private Instant updatedAt;
  private Fraction amountAttemptedPercent;
  private Fraction amountAttempted;
}
