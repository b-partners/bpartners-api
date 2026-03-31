package app.bpartners.api.endpoint.event.model;

import java.time.Duration;
import lombok.*;

@NoArgsConstructor
@Builder
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class AllLogoCompressionTriggered extends PojaEvent {

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(5L);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofMinutes(1L);
  }
}
