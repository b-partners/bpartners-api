package app.bpartners.api.endpoint.event.model;

import static app.bpartners.api.endpoint.event.EventStack.EVENT_STACK_1;

import app.bpartners.api.endpoint.event.EventStack;
import java.time.Duration;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@EqualsAndHashCode(callSuper = false)
@Builder
@Getter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class TransitionalSubscriptionCreditGrantRequested extends PojaEvent {
  private String userId;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofSeconds(300L);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofSeconds(60L);
  }

  @Override
  public EventStack getEventStack() {
    return EVENT_STACK_1;
  }
}
