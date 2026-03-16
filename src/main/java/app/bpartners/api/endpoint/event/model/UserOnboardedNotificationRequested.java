package app.bpartners.api.endpoint.event.model;

import java.time.Duration;
import lombok.*;

@Getter
@EqualsAndHashCode(callSuper = false)
@ToString
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserOnboardedNotificationRequested extends PojaEvent {
  private String userId;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofSeconds(120L);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofSeconds(60L);
  }
}
