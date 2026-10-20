package app.bpartners.api.endpoint.event.model;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnalysisApiKeyCheckTriggeredTest {

  @Test
  void single_user_event_has_no_next_users() {
    var event = new AnalysisApiKeyCheckTriggered("user");

    assertEquals("user", event.getUserId());
    assertEquals(List.of(), event.getNextUserIds());
    assertEquals(Duration.ofMinutes(2), event.maxConsumerDuration());
    assertEquals(Duration.ofMinutes(1), event.maxConsumerBackoffBetweenRetries());
  }

  @Test
  void all_users_event_durations() {
    var event = new AllAnalysisApiKeyCheckTriggered();

    assertEquals(Duration.ofMinutes(5), event.maxConsumerDuration());
    assertEquals(Duration.ofMinutes(1), event.maxConsumerBackoffBetweenRetries());
  }
}
