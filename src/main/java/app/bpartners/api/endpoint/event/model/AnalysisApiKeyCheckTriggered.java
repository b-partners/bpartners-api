package app.bpartners.api.endpoint.event.model;

import java.time.Duration;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = false)
@Getter
public class AnalysisApiKeyCheckTriggered extends PojaEvent {
  private String userId;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(2L);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofMinutes(1L);
  }
}
