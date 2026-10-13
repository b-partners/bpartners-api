package app.bpartners.api.endpoint.event.model;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class SubscriptionUpgradeInvoicingRequestedTest {
  SubscriptionUpgradeInvoicingRequested subject =
      SubscriptionUpgradeInvoicingRequested.builder().subscriptionUpgradeId("upgrade_id").build();

  @Test
  void carries_the_upgrade_identifier() {
    assertEquals("upgrade_id", subject.getSubscriptionUpgradeId());
  }

  @Test
  void bounds_the_consumer_duration_and_backoff() {
    assertEquals(Duration.ofMinutes(5L), subject.maxConsumerDuration());
    assertEquals(Duration.ofMinutes(1L), subject.maxConsumerBackoffBetweenRetries());
  }
}
