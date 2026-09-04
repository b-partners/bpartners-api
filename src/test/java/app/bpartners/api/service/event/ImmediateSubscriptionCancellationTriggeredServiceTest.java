package app.bpartners.api.service.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.ImmediateSubscriptionCancellationRequested;
import app.bpartners.api.endpoint.event.model.ImmediateSubscriptionCancellationTriggered;
import app.bpartners.api.model.User;
import app.bpartners.api.service.subscription.UpcomingDebitedCustomers;
import app.bpartners.api.service.subscription.UpcomingUserDebitService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ImmediateSubscriptionCancellationTriggeredServiceTest {
  UpcomingUserDebitService upcomingUserDebitService = mock();
  EventProducer eventProducer = mock();
  ImmediateSubscriptionCancellationTriggeredService subject =
      new ImmediateSubscriptionCancellationTriggeredService(
          upcomingUserDebitService, eventProducer);

  @Test
  void fans_out_a_cancellation_request_per_billed_user() {
    when(upcomingUserDebitService.getUpcomingDebitedCustomers())
        .thenReturn(
            new UpcomingDebitedCustomers(
                List.of(User.builder().id("user_1").build(), User.builder().id("user_2").build()),
                List.of()));

    subject.accept(new ImmediateSubscriptionCancellationTriggered());

    var captor = ArgumentCaptor.forClass(List.class);
    verify(eventProducer).accept(captor.capture());
    @SuppressWarnings("unchecked")
    var requested = (List<ImmediateSubscriptionCancellationRequested>) captor.getValue();
    assertEquals(
        List.of("user_1", "user_2"),
        requested.stream().map(ImmediateSubscriptionCancellationRequested::getUserId).toList());
  }

  @Test
  void produces_nothing_when_no_user_has_an_active_subscription() {
    when(upcomingUserDebitService.getUpcomingDebitedCustomers())
        .thenReturn(new UpcomingDebitedCustomers(List.of(), List.of()));

    subject.accept(new ImmediateSubscriptionCancellationTriggered());

    verify(eventProducer, never()).accept(any());
  }
}
