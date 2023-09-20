package app.bpartners.api.model.prospect.job;

import app.bpartners.api.endpoint.rest.model.ProspectEvaluationJobStatus;
import app.bpartners.api.endpoint.rest.model.ProspectEvaluationJobType;
import app.bpartners.api.model.prospect.Prospect;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
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
public class ProspectEvaluationJob {
  private String id;
  private String idAccountHolder;
  private ProspectEvaluationJobType type;
  private ProspectEvaluationJobStatus jobStatus;
  private Instant startedAt;
  private Instant endedAt;
  private List<Prospect> results = new ArrayList<>();

  public Duration getDuration() {
    return endedAt == null ? Duration.between(startedAt, Instant.now())
        : Duration.between(startedAt, endedAt);
  }
}
