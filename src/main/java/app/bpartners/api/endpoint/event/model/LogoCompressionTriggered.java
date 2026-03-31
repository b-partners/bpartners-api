package app.bpartners.api.endpoint.event.model;

import java.time.Duration;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = false)
@Getter
public class LogoCompressionTriggered extends PojaEvent {
  private String userId;
  private String userLogoFileId;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(5L);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofMinutes(1L);
  }
}
