package app.bpartners.api.model.prospect.job;

import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class ProspectEvaluationJobRunner implements Serializable {
  private String jobId;
  private EventJobRunner eventJobRunner;

  public boolean isEventConversionJob() {
    return eventJobRunner != null;
  }
}
