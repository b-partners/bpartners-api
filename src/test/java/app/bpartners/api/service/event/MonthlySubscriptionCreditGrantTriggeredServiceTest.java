package app.bpartners.api.service.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.MonthlySubscriptionCreditGrantRequested;
import app.bpartners.api.endpoint.event.model.MonthlySubscriptionCreditGrantTriggered;
import app.bpartners.api.service.subscription.UserSubscriptionProductService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class MonthlySubscriptionCreditGrantTriggeredServiceTest {
  UserSubscriptionProductService userSubscriptionProductService = mock();
  EventProducer eventProducer = mock();
  MonthlySubscriptionCreditGrantTriggeredService subject =
      new MonthlySubscriptionCreditGrantTriggeredService(
          userSubscriptionProductService, eventProducer);

  @Test
  void fans_out_one_grant_request_per_subscribed_user() {
    when(userSubscriptionProductService.findUserIdsWithActiveSubscriptionProduct())
        .thenReturn(List.of("user_1", "user_2"));

    subject.accept(new MonthlySubscriptionCreditGrantTriggered());

    var captor = ArgumentCaptor.forClass(List.class);
    verify(eventProducer).accept(captor.capture());
    @SuppressWarnings("unchecked")
    var requests = (List<MonthlySubscriptionCreditGrantRequested>) captor.getValue();
    assertEquals(2, requests.size());
    assertEquals(
        List.of("user_1", "user_2"),
        requests.stream().map(MonthlySubscriptionCreditGrantRequested::getUserId).toList());
  }

  @Test
  void produces_nothing_when_no_user_is_subscribed() {
    when(userSubscriptionProductService.findUserIdsWithActiveSubscriptionProduct())
        .thenReturn(List.of());

    subject.accept(new MonthlySubscriptionCreditGrantTriggered());

    verify(eventProducer, never()).accept(any());
  }
}
