package app.bpartners.api.endpoint.rest.security;

import app.bpartners.api.endpoint.rest.security.cognito.CognitoComponent;
import app.bpartners.api.endpoint.rest.security.model.Principal;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import app.bpartners.api.service.UserService;

@Component
@AllArgsConstructor
public class AuthProvider extends AbstractUserDetailsAuthenticationProvider {

  private static final String BEARER_PREFIX = "Bearer ";
  private final UserService userService;
  private final CognitoComponent cognitoComponent;

  @Override
  protected void additionalAuthenticationChecks(
      UserDetails userDetails, UsernamePasswordAuthenticationToken token) {
    // nothing
  }

  @Override
  protected UserDetails retrieveUser(
      String username, UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken) {
    String bearer = getBearer(usernamePasswordAuthenticationToken);
    if (bearer == null) {
      throw new UsernameNotFoundException("Bad credentials"); // NOSONAR
    }

    String email = cognitoComponent.getEmailByIdToken(bearer);
    if (email == null) {
      throw new UsernameNotFoundException("Bad credentials"); // NOSONAR
    }

    return new Principal(userService.getByEmail(email), bearer);
  }

  private String getBearer(
      UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken) {
    Object tokenObject = usernamePasswordAuthenticationToken.getCredentials();
    if (!(tokenObject instanceof String) || !((String) tokenObject).startsWith(BEARER_PREFIX)) {
      return null;
    }
    return ((String) tokenObject).substring(BEARER_PREFIX.length()).trim();
  }

  public static Principal getPrincipal() {
    SecurityContext context = SecurityContextHolder.getContext();
    Authentication authentication = context.getAuthentication();
    return (Principal) authentication.getPrincipal();
  }
}
