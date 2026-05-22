package app.bpartners.api.endpoint.event.model;

import java.time.Duration;
import java.time.YearMonth;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class UpcomingDebitedCustomerExportRequested extends PojaEvent {
  private YearMonth yearMonth;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofSeconds(120L);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofSeconds(60L);
  }
}
