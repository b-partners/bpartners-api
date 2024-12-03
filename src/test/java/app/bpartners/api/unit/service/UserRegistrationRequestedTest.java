package app.bpartners.api.unit.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import app.bpartners.api.endpoint.event.model.UserRegistrationRequested;
import app.bpartners.api.model.User;
import app.bpartners.api.model.subscription.UserSubscription;
import app.bpartners.api.service.UserService;
import app.bpartners.api.service.aws.SesService;
import app.bpartners.api.service.event.UserRegistrationRequestedService;
import app.bpartners.api.service.subscription.SubscriptionService;
import org.junit.jupiter.api.Test;

class UserRegistrationRequestedTest {
  UserService userServiceMock = mock(UserService.class);
  SesService mailerMock = mock(SesService.class);
  SubscriptionService subscriptionServiceMock = mock(SubscriptionService.class);
  UserRegistrationRequestedService subject =
      new UserRegistrationRequestedService(subscriptionServiceMock, mailerMock, userServiceMock);

  @Test
  void accept_ok() {
    var user = User.builder().build();
    var event =
        UserRegistrationRequested.builder().userNb(1).totalNbUser(1).userId("userId").build();
    when(userServiceMock.getUserById(any())).thenReturn(user);
    when(subscriptionServiceMock.createUserSubscription(any()))
        .thenReturn(UserSubscription.builder().build());

    assertDoesNotThrow(
        () -> {
          subject.accept(event);
        });
  }
}
