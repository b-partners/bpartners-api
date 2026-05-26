package app.bpartners.api.endpoint.rest.controller;

import static app.bpartners.api.endpoint.rest.security.model.Role.ADMIN_ROLE;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.model.Redirection;
import app.bpartners.api.endpoint.rest.model.RedirectionStatusUrls;
import app.bpartners.api.endpoint.rest.security.cognito.CognitoComponent;
import app.bpartners.api.model.User;
import app.bpartners.api.service.subscription.StripeSetupService;
import app.bpartners.api.service.user.UserService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class UserControllerTest {
  private static final String STRIPE_CUSTOMER_IDENTIFIER = randomUUID().toString();
  StripeSetupService stripeSetupServiceMock = mock();
  CognitoComponent cognitoComponentMock = mock();
  UserService userServiceMock = mock();
  UserController subject =
      new UserController(
          mock(),
          cognitoComponentMock,
          userServiceMock,
          mock(),
          mock(),
          mock(),
          mock(),
          mock(),
          stripeSetupServiceMock);

  @BeforeEach
  void setUp() {
    var email = randomUUID() + "@example.com";
    var userMock = mock(User.class);
    when(userMock.getUserSubscriptionId()).thenReturn(STRIPE_CUSTOMER_IDENTIFIER);
    when(userMock.getRoles()).thenReturn(List.of(ADMIN_ROLE));
    when(cognitoComponentMock.getEmailByToken(any())).thenReturn(email);
    when(userServiceMock.getUserByEmail(email)).thenReturn(userMock);
  }

  @Test
  void initiate_payment_method_url() {
    var successUrl = "http://localhost/" + randomUUID();
    var failureUrl = "http://localhost/" + randomUUID();
    var redirectionUrl = "https://checkout.stripe.com/pay/c/" + randomUUID();
    var userIdentifier = randomUUID().toString();
    var httpServletRequestMock = mock(HttpServletRequest.class);
    when(httpServletRequestMock.getHeader("Authorization"))
        .thenReturn("Bearer " + randomUUID().toString().replace("-", ""));

    var redirectionStatusUrls =
        new RedirectionStatusUrls().successUrl(successUrl).failureUrl(failureUrl);
    when(stripeSetupServiceMock.setupCheckoutSession(
            STRIPE_CUSTOMER_IDENTIFIER, redirectionStatusUrls))
        .thenReturn(
            new Redirection()
                .redirectionStatusUrls(redirectionStatusUrls)
                .redirectionUrl(redirectionUrl));

    var actual =
        subject.initiatePaymentMethodInsertion(
            httpServletRequestMock, userIdentifier, redirectionStatusUrls);

    assertEquals(
        new Redirection()
            .redirectionUrl(redirectionUrl)
            .redirectionStatusUrls(redirectionStatusUrls),
        actual);
  }
}
