package app.bpartners.api.endpoint.rest.security;

import static app.bpartners.api.service.utils.SecurityUtils.BEARER_PREFIX;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import app.bpartners.api.endpoint.rest.security.cognito.CognitoComponent;
import app.bpartners.api.endpoint.rest.security.dashboard.DashboardTokenVerifier;
import app.bpartners.api.endpoint.rest.security.model.BridgePrincipal;
import app.bpartners.api.endpoint.rest.security.model.Principal;
import app.bpartners.api.model.User;
import app.bpartners.api.service.user.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

class BearerAuthenticatorTest {
  private static final String TOKEN = "token";

  CognitoComponent cognitoComponent = mock();
  UserService userService = mock();
  DashboardTokenVerifier dashboardTokenVerifier = mock();
  BearerAuthenticator authenticator =
      new BearerAuthenticator(cognitoComponent, userService, dashboardTokenVerifier);

  private static UsernamePasswordAuthenticationToken bearerCredentials() {
    return new UsernamePasswordAuthenticationToken(BEARER_PREFIX + TOKEN, BEARER_PREFIX + TOKEN);
  }

  @Test
  void dashboard_token_is_verified_without_cognito_or_user_lookup() {
    var bridgePrincipal = new BridgePrincipal("user-id", TOKEN);
    when(dashboardTokenVerifier.isIssuedByDashboard(TOKEN)).thenReturn(true);
    when(dashboardTokenVerifier.verify(TOKEN)).thenReturn(bridgePrincipal);

    var actual = authenticator.retrieveUser(null, bearerCredentials());

    assertSame(bridgePrincipal, actual);
    verifyNoInteractions(cognitoComponent, userService);
  }

  @Test
  void other_issuer_goes_through_cognito() {
    var user = User.builder().id("cognito-user").build();
    when(dashboardTokenVerifier.isIssuedByDashboard(TOKEN)).thenReturn(false);
    when(cognitoComponent.getEmailByToken(TOKEN)).thenReturn("user@mail.com");
    when(userService.getUserByEmail("user@mail.com")).thenReturn(user);

    var actual = (Principal) authenticator.retrieveUser(null, bearerCredentials());

    assertSame(user, actual.getUser());
    verify(cognitoComponent).getEmailByToken(TOKEN);
  }
}
