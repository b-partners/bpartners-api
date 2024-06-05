package app.bpartners.api.endpoint.event.model;

import app.bpartners.api.PojaGenerated;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class UuidCreated extends PojaEvent {
  @JsonProperty("uuid")
  private String uuid;

  @Override
  public Duration maxDuration() {
    return Duration.ofSeconds(10);
  }

  @Override
  public Duration maxBackoffBetweenRetries() {
    return Duration.ofSeconds(30);
  }
}
