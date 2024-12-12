package app.bpartners.api.endpoint.event.model;

import java.time.Duration;
import lombok.*;

@EqualsAndHashCode(callSuper = false)
@Builder
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class MonthlySubscriptionInvoiceRequested extends PojaEvent {
  private Long userPage;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofSeconds(600L);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofSeconds(60L);
  }
}
