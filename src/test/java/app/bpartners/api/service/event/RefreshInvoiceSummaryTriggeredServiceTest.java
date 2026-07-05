package app.bpartners.api.service.event;

import static app.bpartners.api.endpoint.event.EventStack.EVENT_STACK_2;
import static app.bpartners.api.endpoint.rest.model.EnableStatus.DISABLED;
import static app.bpartners.api.endpoint.rest.model.EnableStatus.ENABLED;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.event.EventProducer;
import app.bpartners.api.endpoint.event.model.RefreshInvoiceSummaryTriggered;
import app.bpartners.api.endpoint.event.model.RefreshUserInvoiceSummaryTriggered;
import app.bpartners.api.model.User;
import app.bpartners.api.service.user.UserService;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class RefreshInvoiceSummaryTriggeredServiceTest {

  UserService userServiceMock = mock();
  EventProducer eventProducerMock = mock();
  RefreshInvoiceSummaryTriggeredService subject =
      new RefreshInvoiceSummaryTriggeredService(userServiceMock, eventProducerMock);

  @Test
  void produces_RefreshUserInvoiceSummaryTriggered_for_enable_users() {
    var enableUser = mock(User.class);
    var notEnableUser = mock(User.class);
    var userId = randomUUID().toString();
    when(enableUser.getId()).thenReturn(userId);
    when(enableUser.getStatus()).thenReturn(ENABLED);
    when(enableUser.getUserSubscriptionId()).thenReturn(randomUUID().toString());
    when(notEnableUser.getStatus()).thenReturn(DISABLED);
    when(userServiceMock.findAll()).thenReturn(List.of(enableUser, notEnableUser));

    assertDoesNotThrow(() -> subject.accept(new RefreshInvoiceSummaryTriggered()));

    var listCaptor = ArgumentCaptor.forClass(List.class);
    verify(eventProducerMock, times((1))).accept(listCaptor.capture());
    var capturedRefreshUserInvoiceSummaryTriggered =
        (RefreshUserInvoiceSummaryTriggered) listCaptor.getValue().getFirst();
    assertEquals(
        new RefreshUserInvoiceSummaryTriggered(userId), capturedRefreshUserInvoiceSummaryTriggered);
    assertEquals(EVENT_STACK_2, capturedRefreshUserInvoiceSummaryTriggered.getEventStack());
  }

  @Test
  void does_not_throw_and_keeps_refreshing_other_users_when_one_dispatch_fails() {
    var failingUserId = randomUUID().toString();
    var firstOkUserId = randomUUID().toString();
    var lastOkUserId = randomUUID().toString();
    var failingUser = enabledUser(failingUserId);
    var firstOkUser = enabledUser(firstOkUserId);
    var lastOkUser = enabledUser(lastOkUserId);

    when(userServiceMock.findAll()).thenReturn(List.of(firstOkUser, failingUser, lastOkUser));
    doThrow(new RuntimeException("eventBridge is down"))
        .when(eventProducerMock)
        .accept(argThat(events -> containsRefreshFor(events, failingUserId)));

    assertDoesNotThrow(() -> subject.accept(new RefreshInvoiceSummaryTriggered()));

    verify(eventProducerMock, times(3)).accept(any());
    verify(eventProducerMock).accept(argThat(events -> containsRefreshFor(events, firstOkUserId)));
    verify(eventProducerMock).accept(argThat(events -> containsRefreshFor(events, lastOkUserId)));
  }

  @Test
  void does_not_throw_when_every_dispatch_fails() {
    var firstUser = enabledUser(randomUUID().toString());
    var secondUser = enabledUser(randomUUID().toString());
    when(userServiceMock.findAll()).thenReturn(List.of(firstUser, secondUser));
    doThrow(new RuntimeException("eventBridge is down")).when(eventProducerMock).accept(any());

    assertDoesNotThrow(() -> subject.accept(new RefreshInvoiceSummaryTriggered()));

    verify(eventProducerMock, times(2)).accept(any());
  }

  private static User enabledUser(String userId) {
    var user = mock(User.class);
    when(user.getId()).thenReturn(userId);
    when(user.getStatus()).thenReturn(ENABLED);
    when(user.getUserSubscriptionId()).thenReturn(randomUUID().toString());
    return user;
  }

  @SuppressWarnings("unchecked")
  private static boolean containsRefreshFor(Object events, String userId) {
    return ((List<RefreshUserInvoiceSummaryTriggered>) events)
        .stream()
            .map(RefreshUserInvoiceSummaryTriggered::getUserId)
            .collect(Collectors.toList())
            .contains(userId);
  }
}
