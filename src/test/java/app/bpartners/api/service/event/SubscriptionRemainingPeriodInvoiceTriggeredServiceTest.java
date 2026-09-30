package app.bpartners.api.service.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.SubscriptionRemainingPeriodInvoiceRequested;
import app.bpartners.api.endpoint.event.model.SubscriptionRemainingPeriodInvoiceTriggered;
import app.bpartners.api.repository.UserSubscriptionCommitmentJpaRepository;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SubscriptionRemainingPeriodInvoiceTriggeredServiceTest {
  UserSubscriptionCommitmentJpaRepository userSubscriptionCommitmentRepository = mock();
  EventProducer eventProducer = mock();
  SubscriptionRemainingPeriodInvoiceTriggeredService subject =
      new SubscriptionRemainingPeriodInvoiceTriggeredService(
          userSubscriptionCommitmentRepository, eventProducer);

  @Test
  void fans_out_one_request_per_user_still_committed() {
    when(userSubscriptionCommitmentRepository.findUserIdsWithCommitmentEndingAfter(any()))
        .thenReturn(List.of("first_subscriber_id", "second_subscriber_id"));

    subject.accept(new SubscriptionRemainingPeriodInvoiceTriggered());

    var captor = ArgumentCaptor.forClass(List.class);
    verify(eventProducer).accept(captor.capture());
    var requests = (List<SubscriptionRemainingPeriodInvoiceRequested>) captor.getValue();
    assertEquals(2, requests.size());
    assertEquals("first_subscriber_id", requests.getFirst().getUserId());
    assertEquals("second_subscriber_id", requests.getLast().getUserId());
  }

  @Test
  void fans_out_nothing_when_no_commitment_is_running() {
    when(userSubscriptionCommitmentRepository.findUserIdsWithCommitmentEndingAfter(any()))
        .thenReturn(List.of());

    subject.accept(new SubscriptionRemainingPeriodInvoiceTriggered());

    verify(eventProducer, never()).accept(anyList());
  }

  @Test
  void retries_the_fan_out_for_at_most_five_minutes() {
    var event = new SubscriptionRemainingPeriodInvoiceTriggered();

    assertEquals(Duration.ofMinutes(5L), event.maxConsumerDuration());
    assertEquals(Duration.ofMinutes(1L), event.maxConsumerBackoffBetweenRetries());
  }
}
