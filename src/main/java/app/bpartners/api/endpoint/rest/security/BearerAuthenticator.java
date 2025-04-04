package app.bpartners.api.endpoint.rest.security;

import static app.bpartners.api.endpoint.rest.security.SecurityConf.AUTHORIZATION_HEADER;
import static app.bpartners.api.service.utils.SecurityUtils.BEARER_PREFIX;

import app.bpartners.api.endpoint.rest.security.cognito.CognitoComponent;
import app.bpartners.api.endpoint.rest.security.model.Principal;
import app.bpartners.api.model.User;
import app.bpartners.api.model.UserToken;
import app.bpartners.api.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class BearerAuthenticator implements UsernamePasswordAuthenticator {
  private final CognitoComponent cognitoComponent;
  private final UserService userService;

  @Override
  public UserDetails retrieveUser(
      String username, UsernamePasswordAuthenticationToken authenticationToken) {
    String bearer = getBearerFromHeader(authenticationToken);
    if (bearer == null) {
      throw new UsernameNotFoundException("Bad credentials"); // NOSONAR
    }
    String email = cognitoComponent.getEmailByToken(bearer);
    if (email == null) {
      throw new UsernameNotFoundException("Bad credentials"); // NOSONAR
    }
    User user = userService.getUserByEmail(email);
    UserToken bridgeUserToken = userService.getLatestToken(user);
    bearer = bridgeUserToken == null ? bearer : bridgeUserToken.getAccessToken();

    return new Principal(user, bearer);
  }

  @Override
  public User retrieveUserWithoutLegalFileCheck(HttpServletRequest request) {
    String bearer = request.getHeader(AUTHORIZATION_HEADER);
    if (bearer == null) {
      throw new UsernameNotFoundException("Bad credentials");
    }

    bearer = bearer.substring(BEARER_PREFIX.length()).trim();
    String email = cognitoComponent.getEmailByToken(bearer);
    if (email == null) {
      throw new UsernameNotFoundException("Bad credentials");
    }
    return userService.getUserByEmail(email);
  }

  private String getBearerFromHeader(
      UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken) {
    Object tokenObject = usernamePasswordAuthenticationToken.getCredentials();
    if (!(tokenObject instanceof String) || !((String) tokenObject).startsWith(BEARER_PREFIX)) {
      return null;
    }
    return ((String) tokenObject).substring(BEARER_PREFIX.length()).trim();
  }
}
