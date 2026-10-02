package app.bpartners.api.endpoint.event.model;

import java.time.Duration;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@EqualsAndHashCode(callSuper = false)
@ToString
public class SubscriptionPaymentFailureNotificationRequested extends PojaEvent {
  private String userId;
  private String stripeInvoiceId;
  private String planName;
  private Long amountInCentsWithVat;
  private Instant periodStartDatetime;
  private Instant periodEndDatetime;
  private Instant nextPaymentAttemptDatetime;
  private String paymentUrl;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(2L);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofMinutes(1L);
  }
}
