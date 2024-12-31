package app.bpartners.api.endpoint.event.model;

import static app.bpartners.api.endpoint.event.EventStack.EVENT_STACK_2;

import java.time.Duration;
import app.bpartners.api.endpoint.event.EventStack;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = false)
@Builder
@ToString
@AllArgsConstructor
public class MonthlySubscriptionInvoiceTriggered extends PojaEvent {
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
