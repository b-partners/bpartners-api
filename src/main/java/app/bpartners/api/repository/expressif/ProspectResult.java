package app.bpartners.api.repository.expressif;

import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@EqualsAndHashCode
@ToString
public class ProspectResult<T> {
  private ProspectEval<T> prospectEval;
  private InterventionResult interventionResult;
  private CustomerInterventionResult customerInterventionResult;
  private Instant evaluationDate;

  @Data
  @AllArgsConstructor
  @NoArgsConstructor
  @Builder
  @EqualsAndHashCode
  @ToString
  public static class RuleResult {
    protected Double rating;
    protected Double distance;
    protected String address;
  }

  public static class InterventionResult extends RuleResult {
    public InterventionResult(Double rating, Double distance, String address) {
      super(rating, distance, address);
    }
  }

  public static class CustomerInterventionResult extends RuleResult {
    public CustomerInterventionResult(Double rating, Double distance, String address) {
      super(rating, distance, address);
    }
  }
}
