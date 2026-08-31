package app.bpartners.api.endpoint.event.model;

import app.bpartners.api.model.subscription.BillingInterval;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Duration;
import java.time.Instant;
import lombok.*;

@Builder
@ToString
@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class UserSubscriptionProductBackfillRequested extends PojaEvent {
  @JsonProperty("userId")
  private String userId;

  @JsonProperty("subscriptionProductId")
  private String subscriptionProductId;

  @JsonProperty("billingInterval")
  private BillingInterval billingInterval;

  @JsonProperty("subscriptionStartDatetime")
  private Instant subscriptionStartDatetime;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofMinutes(2);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofMinutes(1);
  }
}
