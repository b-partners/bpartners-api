package app.bpartners.api.endpoint.event.model;

import java.time.Duration;
import java.util.List;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode(callSuper = false)
@Getter
public class AnalysisApiKeyCheckTriggered extends PojaEvent {
  private String userId;

  // users still to check, one at a time, each event emitting the next to avoid HTTP 429
  private List<String> nextUserIds;

  public AnalysisApiKeyCheckTriggered(String userId) {
    this(userId, List.of());
  }

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(2L);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofMinutes(1L);
  }
}
