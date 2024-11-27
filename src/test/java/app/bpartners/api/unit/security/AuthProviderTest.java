package app.bpartners.api.unit.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.endpoint.rest.security.cognito.CognitoComponent;
import app.bpartners.api.endpoint.rest.security.exception.UserSubscriptionExpiredException;
import app.bpartners.api.endpoint.rest.security.model.Principal;
import app.bpartners.api.model.User;
import app.bpartners.api.model.subscription.UserSubscription;
import app.bpartners.api.service.LegalFileService;
import app.bpartners.api.service.UserService;
import app.bpartners.api.service.subscription.SubscriptionService;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

class AuthProviderTest {
  private static final String TOKEN_VALUE = "token";
  CognitoComponent cognitoComponentMock = mock();
  UserService userServiceMock = mock();
  LegalFileService legalFileServiceMock = mock();
  SubscriptionService subscriptionServiceMock = mock();
  AuthProvider subject =
      new AuthProvider(
          cognitoComponentMock, userServiceMock, legalFileServiceMock, subscriptionServiceMock);

  @Test
  void user_authenticated() {
    when(cognitoComponentMock.getEmailByToken(TOKEN_VALUE)).thenReturn("dummyEmail");
    when(userServiceMock.getUserByEmail("dummyEmail")).thenReturn(mockUser());
    when(legalFileServiceMock.getAllToBeApprovedLegalFilesByUserId(any())).thenReturn(List.of());
    var userSubscriptionMock = mock(UserSubscription.class);
    when(userSubscriptionMock.hasValidSubscription()).thenReturn(true);
    when(subscriptionServiceMock.getSubscriptionByUserId(any())).thenReturn(userSubscriptionMock);
    var mockCredentials = "Bearer " + TOKEN_VALUE;
    var usernamePasswordAuthenticationToken =
        new UsernamePasswordAuthenticationToken(mockPrincipal(), mockCredentials);

    assertNotNull(subject.authenticate(usernamePasswordAuthenticationToken));
  }

  @Test
  void user_does_not_have_valid_subscription() {
    when(cognitoComponentMock.getEmailByToken(TOKEN_VALUE)).thenReturn("dummyEmail");
    when(userServiceMock.getUserByEmail("dummyEmail")).thenReturn(mockUser());
    when(legalFileServiceMock.getAllToBeApprovedLegalFilesByUserId(any())).thenReturn(List.of());
    var userSubscriptionMock = mock(UserSubscription.class);
    when(userSubscriptionMock.hasValidSubscription()).thenReturn(false);
    when(subscriptionServiceMock.getSubscriptionByUserId(any())).thenReturn(userSubscriptionMock);
    var mockCredentials = "Bearer " + TOKEN_VALUE;
    var usernamePasswordAuthenticationToken =
        new UsernamePasswordAuthenticationToken(mockPrincipal(), mockCredentials);

    var actual =
        assertThrows(
            UserSubscriptionExpiredException.class,
            () -> subject.authenticate(usernamePasswordAuthenticationToken));

    assertEquals(
        "User.id=null does not have a valid subscription or free trial expired",
        actual.getMessage());
  }

  private @NotNull Principal mockPrincipal() {
    return new Principal(mockUser(), TOKEN_VALUE);
  }

  private User mockUser() {
    return mock(User.class);
  }
}
