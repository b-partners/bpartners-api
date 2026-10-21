package app.bpartners.api.endpoint.event.model;

import static app.bpartners.api.endpoint.event.EventStack.EVENT_STACK_1;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class SubscriptionUpgradeExpirationTriggeredTest {
  SubscriptionUpgradeExpirationTriggered subject = new SubscriptionUpgradeExpirationTriggered();

  @Test
  void bounds_the_consumer_duration_and_backoff() {
    assertEquals(Duration.ofMinutes(5L), subject.maxConsumerDuration());
    assertEquals(Duration.ofMinutes(1L), subject.maxConsumerBackoffBetweenRetries());
  }

  @Test
  void is_consumed_from_the_default_event_stack() {
    assertEquals(EVENT_STACK_1, subject.getEventStack());
    assertEquals("app.bpartners.api.event1", subject.getEventSource());
  }
}
