package app.bpartners.api.endpoint.event.model;

import static java.lang.Math.random;

import app.bpartners.api.PojaGenerated;
import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@PojaGenerated
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class DurablyFallibleUuidCreated extends PojaEvent {
  private UuidCreated uuidCreated;
  private int waitDurationBeforeConsumingInSeconds;
  private double failureRate;

  public boolean shouldFail() {
    return random() < failureRate;
  }

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofSeconds(
        waitDurationBeforeConsumingInSeconds + uuidCreated.maxConsumerDuration().toSeconds());
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return uuidCreated.maxConsumerBackoffBetweenRetries();
  }
}
