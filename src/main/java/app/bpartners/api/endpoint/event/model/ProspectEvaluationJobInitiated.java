package app.bpartners.api.endpoint.event.model;

import app.bpartners.api.model.prospect.job.ProspectEvaluationJobRunner;
import java.io.Serializable;
import java.time.Duration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

// TODO: use generated from EventBridge instead
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class ProspectEvaluationJobInitiated extends PojaEvent {
  private String jobId;
  private String idUser;
  private ProspectEvaluationJobRunner jobRunner;

  @Override
  public Duration maxDuration() {
    return Duration.ofMinutes(3);
  }

  @Override
  public Duration maxBackoffBetweenRetries() {
    return Duration.ofMinutes(1);
  }
}
