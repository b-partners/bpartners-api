package app.bpartners.api.endpoint.event.model;

import app.bpartners.api.model.User;
import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Getter
public class LogoCompressionTriggered extends PojaEvent {
  private User user;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(5L);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofMinutes(1L);
  }
}
