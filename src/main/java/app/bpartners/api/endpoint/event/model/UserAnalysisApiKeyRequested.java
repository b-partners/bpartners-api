package app.bpartners.api.endpoint.event.model;

import app.bpartners.api.model.User;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import lombok.*;

@Data
@EqualsAndHashCode(callSuper = false)
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAnalysisApiKeyRequested extends PojaEvent {
  @JsonProperty("user")
  private User user;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(1);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofMinutes(1);
  }
}
