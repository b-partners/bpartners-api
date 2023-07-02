package app.bpartners.api.expressif;

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
  private Instant evaluationDate;
  private Double rating;
}
