package app.bpartners.api.service.event;

import static app.bpartners.api.model.subscription.SubscriptionTerminationStatus.FAILED;
import static app.bpartners.api.model.subscription.SubscriptionTerminationStatus.PENDING;
import static app.bpartners.api.model.subscription.SubscriptionTerminationStatus.SUBSCRIPTION_ENDED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.PojaEvent;
import app.bpartners.api.endpoint.event.model.SubscriptionTerminationRequested;
import app.bpartners.api.endpoint.event.model.SubscriptionTerminationTriggered;
import app.bpartners.api.model.subscription.SubscriptionTerminationStatus;
import app.bpartners.api.model.subscription.TempSubscriptionTermination;
import app.bpartners.api.repository.jpa.TempSubscriptionTerminationJpaRepository;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SubscriptionTerminationTriggeredServiceTest {
  TempSubscriptionTerminationJpaRepository repository = mock();
  EventProducer eventProducer = mock();
  SubscriptionTerminationTriggeredService subject =
      new SubscriptionTerminationTriggeredService(repository, eventProducer);

  @Test
  void requests_one_termination_per_unfinished_row() {
    when(repository.findByStatusIn(any()))
        .thenReturn(
            List.of(
                TempSubscriptionTermination.builder().id("termination_1").build(),
                TempSubscriptionTermination.builder().id("termination_2").build()));

    subject.accept(new SubscriptionTerminationTriggered());

    var statuses = ArgumentCaptor.forClass(Collection.class);
    verify(repository).findByStatusIn(statuses.capture());
    assertEquals(
        List.of(PENDING, SUBSCRIPTION_ENDED, FAILED),
        List.copyOf((Collection<SubscriptionTerminationStatus>) statuses.getValue()));
    var events = ArgumentCaptor.forClass(List.class);
    verify(eventProducer).accept(events.capture());
    var requested = (List<PojaEvent>) events.getValue();
    assertEquals(
        List.of("termination_1", "termination_2"),
        requested.stream()
            .map(event -> ((SubscriptionTerminationRequested) event).getTerminationId())
            .toList());
  }

  @Test
  void requests_nothing_when_every_row_is_finished() {
    when(repository.findByStatusIn(any())).thenReturn(List.of());

    subject.accept(new SubscriptionTerminationTriggered());

    verifyNoInteractions(eventProducer);
  }
}
