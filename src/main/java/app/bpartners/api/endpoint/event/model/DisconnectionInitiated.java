package app.bpartners.api.endpoint.event.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import javax.annotation.processing.Generated;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Generated("EventBridge")
@Data
@EqualsAndHashCode
@AllArgsConstructor
@ToString
public class DisconnectionInitiated extends PojaEvent {
  private static final long serialVersionUID = 1L;

  @JsonProperty("userId")
  private String userId;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(10);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofMinutes(1);
  }
}
