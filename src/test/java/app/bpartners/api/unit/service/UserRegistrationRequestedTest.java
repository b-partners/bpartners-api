package app.bpartners.api.unit.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.event.model.UserRegistrationRequested;
import app.bpartners.api.model.User;
import app.bpartners.api.model.subscription.UserSubscription;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.event.UserRegistrationRequestedService;
import app.bpartners.api.service.subscription.SubscriptionService;
import app.bpartners.api.service.user.UserService;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

class UserRegistrationRequestedTest {
  UserService userServiceMock = mock(UserService.class);
  SesService mailerMock = mock(SesService.class);
  SubscriptionService subscriptionServiceMock = mock(SubscriptionService.class);
  UserRegistrationRequestedService subject =
      new UserRegistrationRequestedService(subscriptionServiceMock, mailerMock, userServiceMock);

  @SneakyThrows
  @Test
  void accept_ok() {
    var userMock = mock(User.class);
    var userId = "userId";
    var userName = "userName";
    var event = UserRegistrationRequested.builder().userNb(1).totalNbUser(1).userId(userId).build();
    when(userMock.getId()).thenReturn(userId);
    when(userMock.getName()).thenReturn(userName);
    when(userServiceMock.getUserById(any())).thenReturn(userMock);
    when(subscriptionServiceMock.createOrLinkUserSubscription(any()))
        .thenReturn(UserSubscription.builder().user(userMock).build());

    assertDoesNotThrow(() -> subject.accept(event));
    verify(mailerMock).sendEmail(any(), any(), any(), any());
  }
}
