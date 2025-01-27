package app.bpartners.api.endpoint.event.model;

import static app.bpartners.api.endpoint.event.EventStack.EVENT_STACK_2;

import app.bpartners.api.endpoint.event.EventStack;
import java.time.Duration;
import lombok.*;

@EqualsAndHashCode(callSuper = false)
@Builder
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class MonthlySubscriptionInvoiceRequested extends PojaEvent {
  private int userPage;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofSeconds(600L);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofSeconds(60L);
  }

  @Override
  public EventStack getEventStack() {
    return EVENT_STACK_2;
  }
}
