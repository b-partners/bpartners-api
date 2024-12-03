package app.bpartners.api.endpoint.event.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import lombok.*;

@Builder
@ToString
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserRegistrationRequested extends PojaEvent {
  @JsonProperty("userId")
  private String userId;

  @JsonProperty("userNb")
  private int userNb;

  @JsonProperty("totalNbUser")
  private int totalNbUser;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(2);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofMinutes(1);
  }
}
