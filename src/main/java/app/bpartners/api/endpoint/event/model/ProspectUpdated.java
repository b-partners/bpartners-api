package app.bpartners.api.endpoint.event.model;

import app.bpartners.api.model.prospect.Prospect;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
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
@EqualsAndHashCode(callSuper = false)
@ToString
public class ProspectUpdated extends PojaEvent {
  private Prospect prospect;
  private Instant updatedAt;

  @Override
  public Duration maxDuration() {
    return Duration.ofMinutes(1);
  }

  @Override
  public Duration maxBackoffBetweenRetries() {
    return Duration.ofMinutes(1);
  }
}
