package app.bpartners.api.integration;

import static app.bpartners.api.endpoint.rest.security.model.Role.EVAL_PROSPECT;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_ID;
import static app.bpartners.api.integration.conf.utils.TestUtils.JOE_DOE_TOKEN;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.security.AuthProvider;
import app.bpartners.api.endpoint.rest.security.BearerAuthenticator;
import app.bpartners.api.endpoint.rest.security.exception.UserSubscriptionExpiredException;
import app.bpartners.api.endpoint.rest.security.model.Principal;
import app.bpartners.api.integration.conf.MockedThirdParties;
import app.bpartners.api.model.User;
import app.bpartners.api.model.subscription.UserSubscription;
import app.bpartners.api.service.LegalFileService;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@AutoConfigureMockMvc
class AuthProviderIT extends MockedThirdParties {
  private static final String TOKEN_VALUE = "token";
  @MockBean BearerAuthenticator bearerAuthenticatorMock;
  @MockBean LegalFileService legalFileServiceMock;
  @Autowired AuthProvider subject;

  @BeforeEach
  void setUp() {
    when(bearerAuthenticatorMock.retrieveUser(any(), any()))
        .thenReturn(
            new Principal(
                User.builder().roles(List.of(EVAL_PROSPECT)).id(JOE_DOE_ID).build(),
                JOE_DOE_TOKEN));
    when(legalFileServiceMock.getAllToBeApprovedLegalFilesByUserId(any())).thenReturn(List.of());
  }

  @Test
  void user_authenticated() {
    var userSubscriptionMock = mock(UserSubscription.class);
    when(userSubscriptionMock.hasValidSubscription()).thenReturn(true);
    when(subscriptionService.getSubscriptionByUserId(any())).thenReturn(userSubscriptionMock);

    var mockCredentials = "Bearer " + TOKEN_VALUE;
    var usernamePasswordAuthenticationToken =
        new UsernamePasswordAuthenticationToken(mockPrincipal(), mockCredentials);

    assertNotNull(subject.authenticate(usernamePasswordAuthenticationToken));
  }

  @Test
  void user_does_not_have_valid_subscription() {
    var userSubscriptionMock = mock(UserSubscription.class);
    when(userSubscriptionMock.hasValidSubscription()).thenReturn(false);
    when(subscriptionService.getSubscriptionByUserId(any())).thenReturn(userSubscriptionMock);

    var mockCredentials = "Bearer " + TOKEN_VALUE;
    var usernamePasswordAuthenticationToken =
        new UsernamePasswordAuthenticationToken(mockPrincipal(), mockCredentials);

    var actual =
        assertThrows(
            UserSubscriptionExpiredException.class,
            () -> subject.authenticate(usernamePasswordAuthenticationToken));

    assertEquals(
        "User.id=joe_doe_id does not have a valid subscription or free trial expired",
        actual.getMessage());
  }

  private @NotNull Principal mockPrincipal() {
    return new Principal(mockUser(), TOKEN_VALUE);
  }

  private User mockUser() {
    return mock(User.class);
  }
}
