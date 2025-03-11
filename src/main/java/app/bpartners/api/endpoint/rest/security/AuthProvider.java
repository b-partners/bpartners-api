package app.bpartners.api.endpoint.rest.security;

import app.bpartners.api.endpoint.rest.security.model.Principal;
import app.bpartners.api.model.User;
import lombok.AllArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.authentication.dao.AbstractUserDetailsAuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class AuthProvider extends AbstractUserDetailsAuthenticationProvider {
  private final UsernamePasswordAuthenticator authenticator;

  public static Principal getPrincipal() {
    SecurityContext context = SecurityContextHolder.getContext();
    Authentication authentication = context.getAuthentication();
    return (Principal) authentication.getPrincipal();
  }

  public static String getBearer() {
    return userIsAuthenticated() ? getPrincipal().getBearer() : null;
  }

  public static String getAuthenticatedUserId() {
    return userIsAuthenticated() ? getPrincipal().getUserId() : null;
  }

  public static String getPreferredAccountId() {
    return getAuthenticatedUser() != null ? getPrincipal().getUser().getPreferredAccountId() : null;
  }

  public static User getAuthenticatedUser() {
    return userIsAuthenticated() ? getPrincipal().getUser() : null;
  }

  public static boolean userIsAuthenticated() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null) {
      return false;
    } else {
      Object principal = authentication.getPrincipal();
      return !principal.getClass().getTypeName().equals(String.class.getTypeName());
    }
  }

  @Override
  protected void additionalAuthenticationChecks(
      UserDetails userDetails, UsernamePasswordAuthenticationToken token) {
    // nothing
  }

  @Override
  protected UserDetails retrieveUser(
      String username, UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken) {
    return authenticator.retrieveUser(username, usernamePasswordAuthenticationToken);
  }
}
