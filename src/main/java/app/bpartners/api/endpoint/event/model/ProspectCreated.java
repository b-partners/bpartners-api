package app.bpartners.api.endpoint.event.model;

import app.bpartners.api.model.prospect.Prospect;
import java.time.Duration;
import java.time.Instant;
import lombok.*;

// TODO: use generated from EventBridge instead
@Data
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
@ToString
public class ProspectCreated extends PojaEvent {
  private Prospect prospect;
  private String attachmentFileKey;
  private Instant updatedAt;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(1);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofMinutes(1);
  }
}
