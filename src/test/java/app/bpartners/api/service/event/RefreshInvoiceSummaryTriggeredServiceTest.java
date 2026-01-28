package app.bpartners.api.service.event;

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
  }
}
