package app.bpartners.api.service.event;

import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.UserSubscriptionProductBackfillRequested;
import app.bpartners.api.endpoint.event.model.UserSubscriptionProductBackfillTriggered;
import app.bpartners.api.model.User;
import app.bpartners.api.service.subscription.UpcomingDebitedCustomers;
import app.bpartners.api.service.subscription.UpcomingUserDebitService;
import java.util.List;
import org.junit.jupiter.api.Test;

class UserSubscriptionProductBackfillTriggeredServiceTest {
  UpcomingUserDebitService upcomingUserDebitService = mock();
  EventProducer eventProducer = mock();
  UserSubscriptionProductBackfillTriggeredService subject =
      new UserSubscriptionProductBackfillTriggeredService(upcomingUserDebitService, eventProducer);

  @Test
  void fans_out_one_request_event_per_billed_user() {
    var firstUser = User.builder().id(randomUUID().toString()).build();
    var secondUser = User.builder().id(randomUUID().toString()).build();
    when(upcomingUserDebitService.getUpcomingDebitedCustomers())
        .thenReturn(new UpcomingDebitedCustomers(List.of(firstUser, secondUser), List.of()));
    doNothing().when(eventProducer).accept(anyList());

    subject.accept(new UserSubscriptionProductBackfillTriggered());

    verify(eventProducer)
        .accept(
            List.of(
                UserSubscriptionProductBackfillRequested.builder()
                    .userId(firstUser.getId())
                    .build()));
    verify(eventProducer)
        .accept(
            List.of(
                UserSubscriptionProductBackfillRequested.builder()
                    .userId(secondUser.getId())
                    .build()));
  }

  @Test
  void produces_no_event_when_no_billed_user() {
    when(upcomingUserDebitService.getUpcomingDebitedCustomers())
        .thenReturn(new UpcomingDebitedCustomers(List.of(), List.of()));

    subject.accept(new UserSubscriptionProductBackfillTriggered());

    verify(eventProducer, never()).accept(anyList());
  }
}
