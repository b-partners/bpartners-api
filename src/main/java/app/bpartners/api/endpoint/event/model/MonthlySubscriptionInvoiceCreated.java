package app.bpartners.api.endpoint.event.model;

import java.time.Duration;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
@ToString
public class MonthlySubscriptionInvoiceCreated extends PojaEvent {
  private String invoiceId;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofSeconds(300L);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofSeconds(60L);
  }
}
