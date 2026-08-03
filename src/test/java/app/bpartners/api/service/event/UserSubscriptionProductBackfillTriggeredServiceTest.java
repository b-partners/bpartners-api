package app.bpartners.api.service.event;

import static java.util.UUID.randomUUID;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.UserSubscriptionProductBackfillRequested;
import app.bpartners.api.endpoint.event.model.UserSubscriptionProductBackfillTriggered;
import app.bpartners.api.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

class UserSubscriptionProductBackfillTriggeredServiceTest {
  UserRepository userRepository = mock();
  EventProducer eventProducer = mock();
  UserSubscriptionProductBackfillTriggeredService subject =
      new UserSubscriptionProductBackfillTriggeredService(userRepository, eventProducer);

  @Test
  void fans_out_one_request_event_per_enabled_user_with_subscription() {
    var firstUserId = randomUUID().toString();
    var secondUserId = randomUUID().toString();
    when(userRepository.findEnabledUserIdsWithSubscription())
        .thenReturn(List.of(firstUserId, secondUserId));
    doNothing().when(eventProducer).accept(anyList());

    subject.accept(new UserSubscriptionProductBackfillTriggered());

    verify(eventProducer)
        .accept(
            List.of(
                UserSubscriptionProductBackfillRequested.builder().userId(firstUserId).build(),
                UserSubscriptionProductBackfillRequested.builder().userId(secondUserId).build()));
  }

  @Test
  void produces_no_event_when_no_enabled_user_with_subscription() {
    when(userRepository.findEnabledUserIdsWithSubscription()).thenReturn(List.of());

    subject.accept(new UserSubscriptionProductBackfillTriggered());

    verify(eventProducer, never()).accept(anyList());
  }
}
